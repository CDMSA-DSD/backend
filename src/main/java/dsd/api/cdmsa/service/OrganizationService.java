package dsd.api.cdmsa.service;

import com.fasterxml.jackson.databind.JsonNode;
import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.OrganizationRepository;
import dsd.api.cdmsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public OrganizationResponse getOrgDetails(Long id) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();

        // when github is not connected yet, the last three are null and so not displayed in the UI
        return new OrganizationResponse(
                org.getName(),
                org.getDescription(),
                org.getDomain(),
                org.getSelectedRepoName(),
                org.getSelectedBranchName(),
                org.getRepoOwner()
        );
    }

    @Transactional
    public OrganizationResponse updateOrgDetails(Long id, UpdateOrganizationRequest request) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();

        org.setName(request.companyName().trim());
        org.setDescription(request.description().trim());
        org.setDomain(request.domain().trim());
        org.setSelectedRepoName(request.selectedRepoName() != null ? request.selectedRepoName().trim() : null);
        org.setSelectedBranchName(request.selectedBranchName() != null ? request.selectedBranchName().trim() : null);

        Organization updatedOrg = orgRepo.save(org);
        return new OrganizationResponse(
                updatedOrg.getName(),
                updatedOrg.getDescription(),
                updatedOrg.getDomain(),
                updatedOrg.getSelectedRepoName(),
                updatedOrg.getSelectedBranchName(),
                updatedOrg.getRepoOwner()
        );
    }

    @Transactional
    public ConnectGitHubResponse connectGitHub (Long userId, ConnectGitHubRequest request){
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();

        String pat = request.pat().trim();

        // call github API to retrieve the user (organization) account from the pat
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + pat);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid PAT or unable to contact GitHub");
        }

        JsonNode body = response.getBody();
        String githubLogin = body.get("login").asText();
        // name probably not necessary
        String githubName = body.has("name") && !body.get("name").isNull() ? body.get("name").asText() : githubLogin;

        // TODO: encrypt PAT before storing
        org.setGitHubToken(pat);
        org.setRepoOwner(githubLogin);
        orgRepo.save(org);

        return new ConnectGitHubResponse(githubLogin, githubName);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> getRepos(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();

        String pat = org.getGitHubToken();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + pat);
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode[]> response = restTemplate.exchange(
                "https://api.github.com/user/repos",
                HttpMethod.GET,
                entity,
                JsonNode[].class
        );

        return Arrays.stream(response.getBody())
                .map(repo -> new RepositoryResponse(repo.get("name").asText(),
                        repo.get("owner").get("login").asText()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getBranches(Long userId, String owner, String repo) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();
        String pat = org.getGitHubToken();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + pat);
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode[]> response = restTemplate.exchange(
                "https://api.github.com/repos/" + owner + "/" + repo + "/branches",
                HttpMethod.GET,
                entity,
                JsonNode[].class
        );

        return Arrays.stream(response.getBody())
                .map(b -> new BranchResponse(b.get("name").asText()))
                .toList();
    }

    @Transactional
    public OrganizationResponse saveRepoBranchSelection(Long userId, RepoBranchSelectionRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Organization org = user.getOrg();

        org.setSelectedRepoName(request.repoName().trim());
        org.setSelectedBranchName(request.branchName().trim());

        orgRepo.save(org);

        return new OrganizationResponse(
                org.getName(),
                org.getDescription(),
                org.getDomain(),
                org.getSelectedRepoName(),
                org.getSelectedBranchName(),
                org.getRepoOwner()
        );
    }
}
