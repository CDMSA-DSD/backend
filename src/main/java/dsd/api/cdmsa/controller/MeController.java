package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.UpdateUserProfileRequest;
import dsd.api.cdmsa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.assembler.OrgModelAssembler;
import dsd.api.cdmsa.assembler.UserModelAssembler;
import dsd.api.cdmsa.dto.OrganizationResponseAlt;
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
    private final UserService userService;

    private UserModelAssembler userModelAssembler;
    private OrgModelAssembler orgModelAssembler;

    @GetMapping
    public ResponseEntity<EntityModel<UserResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();

        return ResponseEntity.ok(userModelAssembler.toModel(user));
    }

    @GetMapping("/org")
    public ResponseEntity<EntityModel<OrganizationResponseAlt>> getMyOrganization(
            @AuthenticationPrincipal UserPrincipal principal) {

        Organization org = orgService.getOrgDetailsAlt(principal.getOrgId());

        return ResponseEntity.ok(orgModelAssembler.toModel(org));
    }

    // Update my profile
    @PutMapping()
    public ResponseEntity<EntityModel<UserResponse>> updateMyProfile(
            @RequestBody @Valid UpdateUserProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal.getUser().getId();
        User response = userService.updateUserProfile(userId, request);

        return ResponseEntity.ok(userModelAssembler.toModel(response));
    }

}
