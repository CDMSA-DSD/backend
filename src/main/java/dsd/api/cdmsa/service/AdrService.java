package dsd.api.cdmsa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dsd.api.cdmsa.dto.AdrResponse;
import dsd.api.cdmsa.dto.CreateAdrRequest;
import dsd.api.cdmsa.dto.UpdateAdrRequest;
import dsd.api.cdmsa.model.*;
import dsd.api.cdmsa.repository.AdrRepository;
import dsd.api.cdmsa.repository.RfcRepository;
import dsd.api.cdmsa.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AdrService {

    private final AdrRepository adrRepository;
    private final RfcRepository rfcRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AdrResponse createAdr(CreateAdrRequest request, Long userId) {
        // eventually put some checks on the request (but we used @Valid so maybe not needed) ...

        RFC rfc = rfcRepository.findById(request.rfcId())
                .orElseThrow(() -> new EntityNotFoundException("RFC not found with id " + request.rfcId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();

        ADR adr = new ADR();
        adr.setTitle(request.title().trim());
        adr.setContext(request.context().trim());
        adr.setDecision(request.decision().trim());
        adr.setConsequences(request.consequences().trim());				// from this create markdown file, generate and store the url of GitHub in the db
        adr.setStatus(request.status());
        adr.setRfc(rfc);
        ADR saved = adrRepository.save(adr);

        String markdown = buildMarkdown(saved);

        // push the markdown on github
        String filePath = "adr-" + saved.getId() + ".md";
        String githubUrl;
        try {
            githubUrl = pushMarkdownToGitHub(markdown, filePath, org, saved.getTitle());
        } catch (Exception e) {
            throw new RuntimeException("Failed to push ADR to GitHub", e);
        }

        // save url in the db
        saved.setGitHubUrl(githubUrl);
        saved = adrRepository.save(saved);


        return new AdrResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getContext(),
                saved.getDecision(),
                saved.getConsequences(),
                saved.getStatus(),
                saved.getRfc().getId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getGitHubUrl()
        );
    }

    @Transactional (readOnly = true)
    public AdrResponse getAdrById(Long id) {

        ADR adr = adrRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("RFC not found with id " + id));

        return new AdrResponse(
                adr.getId(),
                adr.getTitle(),
                adr.getContext(),
                adr.getDecision(),
                adr.getConsequences(),
                adr.getStatus(),
                adr.getRfc().getId(),
                adr.getCreatedAt(),
                adr.getUpdatedAt(),
                adr.getGitHubUrl()
        );
    }


    @Transactional(readOnly = true)
    public Page<AdrResponse> listAdrs(Pageable pageable) {
        return adrRepository.findAll(pageable)
                .map(adr -> new AdrResponse(
                        adr.getId(),
                        adr.getTitle(),
                        adr.getContext(),
                        adr.getDecision(),
                        adr.getConsequences(),
                        adr.getStatus(),
                        adr.getRfc().getId(),
                        adr.getCreatedAt(),
                        adr.getUpdatedAt(),
                        adr.getGitHubUrl()
                ));
    }

    @Transactional
    public ADR createDraftFromRfcAndAlternative(RFC rfc, Alternative alternative) {
            ADR adr = new ADR();
            adr.setRfc(rfc);
            adr.setStatus(ADR.Status.DRAFT);
            adr.setTitle("ADR for RFC #" + rfc.getId() + ": " + rfc.getTitle());
            adr.setContext(rfc.getDescription() != null ? rfc.getDescription() : "");
            adr.setDecision("Selected alternative: " + (alternative != null ? alternative.getTitle() : ""));
            adr.setConsequences("");
            return adrRepository.save(adr);
    }

    @Transactional
    public AdrResponse updateAdr(Long id, UpdateAdrRequest request, Long userId) {
        ADR adr = adrRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ADR not found with id " + id));

        adr.setTitle(request.title() != null ? request.title().trim() : null);
        adr.setContext(request.context() != null ? request.context().trim() : null);
        adr.setDecision(request.decision() != null ? request.decision().trim() : null);
        adr.setConsequences(request.consequences() != null ? request.consequences().trim() : null);
        adr.setStatus(request.status());

        ADR saved = adrRepository.save(adr);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Organization org = user.getOrg();

        String markdown = buildMarkdown(saved);
        String filePath = "adr-" + saved.getId() + ".md";

        try {
            updateMarkdownOnGitHub(markdown, filePath, org, saved.getTitle());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update ADR on GitHub", e);
        }

        return new AdrResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getContext(),
                saved.getDecision(),
                saved.getConsequences(),
                saved.getStatus(),
                saved.getRfc().getId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getGitHubUrl()
        );
    }


    // GITHUB RELATED METHODS
    public String buildMarkdown(ADR adr) {
        return """
    # %s

    ## Context
    %s

    ## Decision
    %s

    ## Consequences
    %s

    ## Status
    %s
    """.formatted(
                adr.getTitle(),
                adr.getContext(),
                adr.getDecision(),
                adr.getConsequences(),
                adr.getStatus()
        );
    }

    // push on GitHub
    private String pushMarkdownToGitHub(String markdownContent, String filePath, Organization org, String title) throws Exception {
        // get this from org
        String token = org.getGitHubToken();
        String repoOwner = org.getRepoOwner();
        String repoName = org.getSelectedRepoName();
        String branchName = org.getSelectedBranchName();

        String url = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + filePath;
        String contentBase64 = Base64.getEncoder().encodeToString(
                markdownContent.getBytes(StandardCharsets.UTF_8));

        // Crea il body della richiesta
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("message", "Published ADR - " + title);
        requestBody.put("content", contentBase64);
        requestBody.put("branch", branchName);

        // Configura gli headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Effettua la richiesta PUT
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class
        );

        if (response.getStatusCode().value() >= 400) {
            throw new RuntimeException("GitHub API error: " + response.getStatusCode()
                    + " - " + response.getBody());
        }

        // Parsing della risposta
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        return rootNode.get("content").get("html_url").asText();

    }


    // update a .md file already existing
    private void updateMarkdownOnGitHub(String markdownContent, String filePath,
                                        Organization org, String title) throws Exception {
        // get this from org
        String token = org.getGitHubToken();
        String repoOwner = org.getRepoOwner();
        String repoName = org.getSelectedRepoName();
        String branchName = org.getSelectedBranchName();

        String url = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + filePath;

        String sha = getFileSha(url, token);
        if (sha == null) {
            throw new RuntimeException("Cannot update file: file not found on GitHub");
        }

        String contentBase64 = Base64.getEncoder().encodeToString(
                markdownContent.getBytes(StandardCharsets.UTF_8));

        // Crea il body della richiesta
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("message", "Updated ADR - " + title);
        requestBody.put("content", contentBase64);
        requestBody.put("sha", sha);
        requestBody.put("branch", branchName);

        // Configura gli headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Effettua la richiesta PUT
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class
        );

        if (response.getStatusCode().value() >= 400) {
            throw new RuntimeException("GitHub API error: " + response.getStatusCode()
                    + " - " + response.getBody());
        }

    }

    private String getFileSha(String url, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Effettua la richiesta GET
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().value() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                return rootNode.get("sha").asText();
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw e;
        }

        return null;
    }
}

