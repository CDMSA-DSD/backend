package dsd.api.cdmsa.controller;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.assembler.OrgModelAssembler;
import dsd.api.cdmsa.assembler.UserModelAssembler;
import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.dto.UserResponse;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.service.OrgService;
import lombok.AllArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/me")
@AllArgsConstructor
public class MeController {

    private final OrgService orgService;

    private UserModelAssembler userModelAssembler;
    private OrgModelAssembler orgModelAssembler;

    @GetMapping
    public ResponseEntity<EntityModel<UserResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();

        return ResponseEntity.ok(userModelAssembler.toModel(user));
    }

    @GetMapping("/org")
    public ResponseEntity<EntityModel<OrganizationResponse>> getMyOrganization(
            @AuthenticationPrincipal UserPrincipal principal) {

        Organization org = orgService.getOrgDetails(principal.getOrgId());

        return ResponseEntity.ok(orgModelAssembler.toModel(org));
    }

}
