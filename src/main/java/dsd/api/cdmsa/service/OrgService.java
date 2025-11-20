package dsd.api.cdmsa.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Map;
import java.util.List;
import java.util.Arrays;

import org.springframework.data.domain.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.databind.JsonNode;

import dsd.api.cdmsa.controller.UserController;
import dsd.api.cdmsa.dto.OrgAdminRequest;
import dsd.api.cdmsa.dto.OrgAdminResponse;
import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.dto.SignInRequest;
import dsd.api.cdmsa.dto.UpdateOrganizationRequest;
import dsd.api.cdmsa.exception.OrgExistsException;
import dsd.api.cdmsa.exception.OrgNotFoundException;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.OrganizationRepository;
import dsd.api.cdmsa.repository.UserRepository;
import dsd.api.cdmsa.dto.*;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrgService {

    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;
    private final UserService userService;

    public boolean existOrg(String name) {
        return orgRepo.existsByName(name);
    }

    public boolean existOrgById(Long id) {
        return orgRepo.existsById(id);
    }

    @Transactional
    public OrgAdminResponse createOrg(OrgAdminRequest dto) {

        OrganizationResponse org = dto.org();
        SignInRequest user = dto.admin();

        // Check if a Org already exist
        if (!existOrg(org.companyName())) {

            // Creates the org (set all the parameters)
            Organization newOrg = new Organization();
            newOrg.setName(org.companyName());
            newOrg.setDescription(org.description());
            newOrg.setDomain(org.domain());

            // Stores temporal
            newOrg = orgRepo.save(newOrg);

            // Creates the admin (set all the parameters)
            User admin = new User();
            admin.setFirstname(user.firstname());
            admin.setLastname(user.lastname());
            admin.setEmail(user.email());
            admin.setPassword(user.password()); // Hashed later in createUser

            admin.setOrg(newOrg); // Admin belongs to org

            // Stores both resources
            admin = userService.createUser(admin);

            newOrg.setAdminUser(admin); // Admin manages org

            newOrg = orgRepo.save(newOrg); // Updated org

            // Create the admin's URI
            Map<String, Object> adminUri = Map.of(
                    "admin", Map.of(
                            "href", linkTo(UserController.class).slash(admin.getId()).toUri().toString()));

            return new OrgAdminResponse(newOrg.getId(), adminUri);

        }
        // Instead throw a exception that return 409- CONFLICT
        throw new OrgExistsException(org.companyName());
    }

    public void deleteOrg(Long id) {
        orgRepo.deleteById(id);
    }

    public Page<Organization> findAllOrgs(Long orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orgRepo.findById(orgId, pageable);
    }

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

    @Transactional(readOnly = true)
    public Organization getOrgDetailsAlt(Long orgId) {
        return orgRepo.findById(orgId).orElseThrow(() -> new OrgNotFoundException(orgId));
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
