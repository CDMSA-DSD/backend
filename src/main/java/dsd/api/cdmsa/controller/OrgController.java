package dsd.api.cdmsa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

import dsd.api.cdmsa.assembler.OrgModelAssembler;
import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.service.OrgService;

import lombok.AllArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/orgs")
@AllArgsConstructor
public class OrgController {

    private final OrgService service;

    private OrgModelAssembler orgModelAssembler;
    private PagedResourcesAssembler<Organization> pagedResourcesAssembler;

    private UserPrincipal getAuthenticatedPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        throw new UserNotFoundException("anonymous");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            return getAuthenticatedPrincipal().getUser().getId();
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new UserNotFoundException("anonymous");
        }
    }

    // GET Org (individual)
    @GetMapping(value = "/{id}")
    public ResponseEntity<EntityModel<OrganizationResponse>> getOrg(@PathVariable Long id) {
        Organization org = service.getOrgDetails(id);
        return ResponseEntity.ok(orgModelAssembler.toModel(org));
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<OrganizationResponse>>> getAllUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "2", required = false) int size) {

        Page<Organization> orgs = service.findAllOrgs(principal.getOrgId(), page, size);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(orgs, orgModelAssembler));
    }

    // Get organization details
    @GetMapping
    public ResponseEntity<OrganizationResponse> getOrgDetails(HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        OrganizationResponse org = orgService.getOrgDetails(userId);
        return ResponseEntity.ok(org);
    }

    // Update organization details
    @PutMapping
    public ResponseEntity<OrganizationResponse> updateOrgDetails(@Valid @RequestBody UpdateOrganizationRequest request, HttpServletRequest httpRequest) {
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