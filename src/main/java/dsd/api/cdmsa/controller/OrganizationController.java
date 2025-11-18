package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/org_details")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;

    /**
     * Retrieves the current user id from the request.
     * For this sprint we use a simple header-based approach ("X-User-Id"),
     * which should be replaced by a proper authentication mechanism later.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            // For development/demo purposes only.
            // In production, this must be replaced by authenticated user context.
            return 1L;
        }
        return Long.parseLong(header);
    }

    // Get organization details
    @GetMapping
    public ResponseEntity<OrganizationResponse> getOrgDetails(HttpServletRequest httpRequest) {
        // TO DO - check if user is logged ...
        Long userId = getCurrentUserId(httpRequest);
        OrganizationResponse org = orgService.getOrgDetails(userId);
        return ResponseEntity.ok(org);
    }

    // Update organization details
    @PutMapping
    public ResponseEntity<OrganizationResponse> updateOrgDetails(@Valid @RequestBody UpdateOrganizationRequest request, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged ...
        Long userId = getCurrentUserId(httpRequest);
        OrganizationResponse orgUpdated = orgService.updateOrgDetails(userId, request);
        return ResponseEntity.ok(orgUpdated);
    }

    // connect GitHub through pat
    @PostMapping("/github")
    public ResponseEntity<ConnectGitHubResponse> connectGitHub(@Valid @RequestBody ConnectGitHubRequest request, HttpServletRequest httpRequest){
        // check id user is admin
        Long userId = getCurrentUserId(httpRequest);
        ConnectGitHubResponse resp = orgService.connectGitHub(userId, request);
        return ResponseEntity.ok(resp);
    }

    // get the repos of the organization, to show them in a dropdown menu and select one
    @GetMapping("/github/repos")
    public ResponseEntity<List<RepositoryResponse>> getRepos(HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(orgService.getRepos(userId));
    }

    // get the branches of a certain repo of the organization, to show them in a dropdown menu and select one
    // check if passing owner is necessary, we have only one owner?
    @GetMapping("/github/{owner}/{repo}/branches") // owner and repo sent from the frontend, we have them from getRepos
    public ResponseEntity<List<BranchResponse>> getBranches(@PathVariable String owner,
                                                            @PathVariable String repo,
                                                            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(orgService.getBranches(userId, owner, repo));
    }

    // choose repo e branch where to store ADRs
    @PutMapping("/github/selection")
    public ResponseEntity<OrganizationResponse> saveRepoBranchSelection(
            @Valid @RequestBody RepoBranchSelectionRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        OrganizationResponse response = orgService.saveRepoBranchSelection(userId, request);
        return ResponseEntity.ok(response);
    }
}
