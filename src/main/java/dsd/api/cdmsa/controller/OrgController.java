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

}