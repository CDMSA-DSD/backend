package dsd.api.cdmsa.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.assembler.InvitationModelAssembler;
import dsd.api.cdmsa.dto.InvitationResponse;
import dsd.api.cdmsa.dto.LinkResponse;
import dsd.api.cdmsa.model.OrganizationInvitation;

import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.service.OrganizationInvitationService;

import lombok.AllArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/invitations")
@AllArgsConstructor
public class OrganizationInvitationController {

    private final OrganizationInvitationService service;

    private InvitationModelAssembler invitationModelAssembler;
    // private PagedResourcesAssembler<OrganizationInvitation> pagedResourcesAssembler;

    @PostMapping
    @PreAuthorize("@permissionService.canManageOrg(principal)")
    public ResponseEntity<LinkResponse> createLink(@AuthenticationPrincipal UserPrincipal principal) {
       
        LinkResponse link = service.createGeneralInvitationLink(principal);

        return ResponseEntity.created(linkTo(methodOn(OrganizationInvitationController.class).getInvitation(link.id())).toUri()).body(link);
    }

    @GetMapping (value = "/{id}")
    @PreAuthorize("@permissionService.canManageOrg(principal)")
    public ResponseEntity<EntityModel<InvitationResponse>> getInvitation(@PathVariable Long id) {
        OrganizationInvitation invitation = service.getInvitation(id);
        return ResponseEntity.ok(invitationModelAssembler.toModel(invitation));
    }

    @GetMapping
    @PreAuthorize("@permissionService.canManageOrg(principal)")
    public ResponseEntity<PagedModel<EntityModel<InvitationResponse>>> getAllInvitations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "2", required = false) int size,
            PagedResourcesAssembler<OrganizationInvitation> pagedResourcesAssembler) {

        Page<OrganizationInvitation> users = service.findAllInvitations(principal.getOrgId(),page, size);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(users, invitationModelAssembler));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        service.deleteInvitation(id, principal);

        return ResponseEntity.noContent().build();
    }
    
}
