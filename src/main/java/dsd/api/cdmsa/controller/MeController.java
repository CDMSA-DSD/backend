package dsd.api.cdmsa.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import dsd.api.cdmsa.dto.UpdateUserProfileRequest;
import dsd.api.cdmsa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import dsd.api.cdmsa.assembler.NotificationResponseModelAssembler;
import dsd.api.cdmsa.assembler.OrgModelAssembler;
import dsd.api.cdmsa.assembler.UserResponseModelAssembler;
import dsd.api.cdmsa.dto.ContextByAdminResponse;
import dsd.api.cdmsa.dto.NotificationResponse;
import dsd.api.cdmsa.dto.NotificationStatusResponse;
import dsd.api.cdmsa.dto.OrganizationResponseAlt;
import dsd.api.cdmsa.dto.UserResponse;
import dsd.api.cdmsa.model.Notification;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.service.ContextService;
import dsd.api.cdmsa.service.NotificationService;
import dsd.api.cdmsa.service.OrgService;
import lombok.AllArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/me")
@AllArgsConstructor
public class MeController {

    private final OrgService orgService;
    private final NotificationService notiService;
    private final UserService userService;
    private final ContextService contextService;

    private UserResponseModelAssembler userResponseModelAssembler;
    private OrgModelAssembler orgModelAssembler;
    private NotificationResponseModelAssembler notiResponseModelAssembler;

    private PagedResourcesAssembler<Notification> notiPagedResourcesAssembler;

    @GetMapping
    public ResponseEntity<EntityModel<UserResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();

        return ResponseEntity.ok(userResponseModelAssembler.toModel(user));
    }

    @GetMapping("/org")
    public ResponseEntity<EntityModel<OrganizationResponseAlt>> getMyOrganization(
            @AuthenticationPrincipal UserPrincipal principal) {

        Organization org = orgService.getOrgDetailsAlt(principal.getOrgId());

        return ResponseEntity.ok(orgModelAssembler.toModel(org));
    }

    @GetMapping("/notifications")
    public ResponseEntity<PagedModel<EntityModel<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "2", required = false) int size,
            @RequestParam(defaultValue = "0", required = false) int page) {

        Page<Notification> notifications = notiService.findAllNotificationByUserId(principal.getUser(), read, page, size);

        return ResponseEntity.ok(notiPagedResourcesAssembler.toModel(notifications, notiResponseModelAssembler));
    }

    @GetMapping("/notifications/status")
    public NotificationStatusResponse getMyNotificationStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean read = notiService.areThereNotReadNotis(principal.getUser());
        long notisNotRead = notiService.countNotReadNotis(principal.getUser());

        return new NotificationStatusResponse(read, notisNotRead);
    }

    @PatchMapping("/notifications/{notiId}/read")
    public ResponseEntity<Void> markNotiAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notiId) {

        notiService.markNotiAsRead(principal.getUser(), notiId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<Void> markAllNotisAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notiService.markAllNotisAsRead(principal.getUser());

        return ResponseEntity.noContent().build();
    }

    // Update my profile
    @PutMapping()
    public ResponseEntity<EntityModel<UserResponse>> updateMyProfile(
            @RequestBody @Valid UpdateUserProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal.getUser().getId();
        User response = userService.updateUserProfile(userId, request);

        return ResponseEntity.ok(userResponseModelAssembler.toModel(response));
    }

    // Get context IDs where the user is an admin
    @GetMapping("/contexts/admin")
    public ResponseEntity<List<ContextByAdminResponse>> getAdminContextIds(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        List<ContextByAdminResponse> contextIds = contextService.findContextByAdmin(user);
        
        return ResponseEntity.ok(contextIds);
    }

}
