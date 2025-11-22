package dsd.api.cdmsa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import dsd.api.cdmsa.dto.AddContextMemberRequest;
import dsd.api.cdmsa.dto.ContextAdminResponse;
import dsd.api.cdmsa.dto.ContextMemberResponse;
import dsd.api.cdmsa.dto.ContextResponse;
import dsd.api.cdmsa.dto.CreateContextRequest;
import dsd.api.cdmsa.dto.PromoteContextAdminRequest;
import dsd.api.cdmsa.dto.UpdateContextRequest;
import dsd.api.cdmsa.service.ContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.UserPrincipal;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/contexts")
@RequiredArgsConstructor
public class ContextController {

    private final ContextService contextService;

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

    // ===========================================================================
    // US-08 Endpoints
    // ===========================================================================

    /*
     * POST /contexts
     * Creates a new context for the current user's organization.
     */

    @PostMapping
    public ResponseEntity<ContextResponse> createContext(
            @RequestBody CreateContextRequest request,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        ContextResponse response = contextService.createContext(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * GET /contexts
     * Lists all contexts for the user's organization.
     */

    @GetMapping
    public ResponseEntity<List<ContextResponse>> listContexts(
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        List<ContextResponse> contexts = contextService.listContexts(userId);
        return ResponseEntity.ok(contexts);
    }

    /*
     * GET /contexts/{id}
     * Retrieves a specific context from the user's organization.
     */

    @GetMapping("/{id}")
    public ResponseEntity<ContextResponse> getContext(
            @PathVariable Long id,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        ContextResponse context = contextService.getContext(userId, id);
        return ResponseEntity.ok(context);
    }

    /*
     * PUT /contexts/{id}
     * Updates a context (admin only).
     */

    @PutMapping("/{id}")
    public ResponseEntity<ContextResponse> updateContext(
            @PathVariable Long id,
            @RequestBody UpdateContextRequest request,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        ContextResponse updated = contextService.updateContext(userId, id, request);
        return ResponseEntity.ok(updated);
    }

    /*
     * DELETE /contexts/{id}
     * Soft deletes a context (admin only).
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContext(
            @PathVariable Long id,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);
        contextService.deleteContext(userId, id);
        return ResponseEntity.noContent().build();
    }

    // =================================================================
    // US-10: Context Admins
    // =================================================================

    /*
     * POST /contexts/{contextId}/admins
     * Promotes a user to Context Admin.
     * Only organization admins can perform this action.
     */

    @PostMapping("/{contextId}/admins")
    public ResponseEntity<ContextAdminResponse> promoteContextAdmin(
            @PathVariable Long contextId,
            @RequestBody PromoteContextAdminRequest request,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);

        ContextAdminResponse response = contextService.promoteContextAdmin(userId, contextId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * DELETE /contexts/{contextId}/admins/{targetUserId}
     * Demotes a Context Admin back to a regular context member.
     * Only organization admins can perform this action.
     */

    @DeleteMapping("/{contextId}/admins/{targetUserId}")
    public ResponseEntity<Void> demoteContextAdmin(
            @PathVariable Long contextId,
            @PathVariable Long targetUserId,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);

        contextService.demoteContextAdmin(userId, contextId, targetUserId);

        return ResponseEntity.noContent().build();
    }

    /*
     * GET /contexts/{contextId}/admins
     * Lists all Context Admins of the context.
     * Any user of the organization may view, unless you want to restrict it.
     */

    @GetMapping("/{contextId}/admins")
    public ResponseEntity<List<ContextAdminResponse>> listContextAdmins(
            @PathVariable Long contextId,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);

        List<ContextAdminResponse> admins = contextService.listContextAdmins(userId, contextId);

        return ResponseEntity.ok(admins);
    }

    // ===================================================
    // US-09: Context Members
    // ===================================================

    /*
     * POST /contexts/{contextId}/members
     * Adds a user to a context
     * Only organization and context admins can perform this action.
     */

    @PostMapping("/{contextId}/members")
    public ResponseEntity<ContextMemberResponse> addMember(
            @PathVariable Long contextId,
            @RequestBody AddContextMemberRequest request,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);

        ContextMemberResponse response = contextService.addMember(userId, contextId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * DELETE /contexts/{contextId}/members/{userId}
     * Eliminates an user from a context
     * Only organization and context admins can perform this action.
     */

    @DeleteMapping("/{contextId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long contextId,
            @PathVariable Long targetUserId,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);

        contextService.removeMember(userId, contextId, targetUserId);

        return ResponseEntity.noContent().build();
    }

    /*
     * GET /contexts/{contextId}/members
     * Lists all the members of a context
     * Only organization and context admins can perform this action.
     */

    @GetMapping("/{contextId}/members")
    public ResponseEntity<List<ContextMemberResponse>> listMembers(
            @PathVariable Long contextId,
            HttpServletRequest http) {
        Long userId = getCurrentUserId(http);

        List<ContextMemberResponse> members = contextService.listMembers(userId, contextId);

        return ResponseEntity.ok(members);
    }

}
