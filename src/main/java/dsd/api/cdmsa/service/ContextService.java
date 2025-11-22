package dsd.api.cdmsa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dsd.api.cdmsa.dto.AddContextMemberRequest;
import dsd.api.cdmsa.dto.ContextAdminResponse;
import dsd.api.cdmsa.dto.ContextByAdminResponse;
import dsd.api.cdmsa.dto.ContextMemberResponse;
import dsd.api.cdmsa.dto.ContextResponse;
import dsd.api.cdmsa.dto.CreateContextRequest;
import dsd.api.cdmsa.dto.PromoteContextAdminRequest;
import dsd.api.cdmsa.dto.UpdateContextRequest;
import dsd.api.cdmsa.exception.ContextBadRequestException;
import dsd.api.cdmsa.exception.ContextForbiddenException;
import dsd.api.cdmsa.exception.ContextNotFoundException;
import dsd.api.cdmsa.model.Context;
import dsd.api.cdmsa.model.ContextMembership;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.ContextMembershipRepository;
import dsd.api.cdmsa.repository.ContextRepository;
import dsd.api.cdmsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContextService {

    private final ContextRepository contextRepository;
    private final UserRepository userRepository;
    private final ContextMembershipRepository membershipRepository;

    // =============================================================================
    // useful methods
    // =============================================================================

    // Loads the current user and ensures it exists.

    private User getCurrentUserOrThrow(Long userId) {
        if (userId == null) {
            throw new ContextBadRequestException("User id is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ContextForbiddenException("User not found or not authenticated"));
    }

    // Returns the organization for the current user, or throws if none.

    private Organization getUserOrganizationOrThrow(User user) {
        Organization org = user.getOrg();
        if (org == null) {
            throw new ContextBadRequestException("User is not assigned to any organization");
        }
        return org;
    }

    // Checks whether the given user is the admin of the given organization.

    private boolean isOrganizationAdmin(User user, Organization org) {
        return org.getAdminUser() != null
                && org.getAdminUser().getId().equals(user.getId());
    }

    private void ensureOrganizationAdmin(User user, Organization org) {
        if (!isOrganizationAdmin(user, org)) {
            throw new ContextForbiddenException("Only organization administrators can perform this operation");
        }
    }

    private Context loadContextOrThrow(Long contextId) {
        return contextRepository.findById(contextId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found"));
    }

    // returns true if user can manage members of this contexts (org admins or
    // (this) context admins)
    private boolean canManageContextMembers(User user, Context context) {
        Organization org = context.getOrganization();

        // Org admin can always manage
        if (isOrganizationAdmin(user, org)) {
            return true;
        }

        // Context admin can manage only this context
        return membershipRepository.existsByContextIdAndUserIdAndContextAdminTrue(
                context.getId(), user.getId());
    }

    private void ensureCanManageContextMembers(User user, Context context) {
        if (!canManageContextMembers(user, context)) {
            throw new ContextForbiddenException(
                    "Only organization admins or context admins can manage members");
        }
    }

    // ============================================================================
    // US-08
    // ============================================================================

    // ---------- US-08: Create Context ----------

    @Transactional
    public ContextResponse createContext(Long userId, CreateContextRequest request) {

        if (request == null) {
            throw new ContextBadRequestException("Request body cannot be null");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ContextBadRequestException("Context name is required");
        }

        User user = getCurrentUserOrThrow(userId);
        Organization org = getUserOrganizationOrThrow(user);
        ensureOrganizationAdmin(user, org);

        String trimmedName = request.name().trim();

        // Validate unique name within organization
        boolean exists = contextRepository.existsByOrganizationIdAndNameIgnoreCaseAndActiveTrue(
                org.getId(), trimmedName);
        if (exists) {
            throw new ContextBadRequestException(
                    "A context with this name already exists in the organization");
        }

        Context context = new Context();

        context.setOrganization(org);
        context.setName(trimmedName);
        context.setType(request.type());
        context.setDescription(request.description());
        context.setActive(true);
        context.setCreatedAt(java.time.Instant.now());

        Context saved = contextRepository.save(context);
        return ContextResponse.fromEntity(saved);
    }

    // ---------- US-08: List Contexts ----------

    @Transactional(readOnly = true)
    public List<ContextResponse> listContexts(Long userId) {
        User user = getCurrentUserOrThrow(userId);
        Organization org = getUserOrganizationOrThrow(user);

        return contextRepository.findByOrganizationIdAndActiveTrue(org.getId())
                .stream()
                .map(ContextResponse::fromEntity)
                .toList();
    }

    // ---------- US-08: Get a single Context ----------

    @Transactional(readOnly = true)
    public ContextResponse getContext(Long userId, Long contextId) {
        if (contextId == null) {
            throw new ContextBadRequestException("Context id is required");
        }

        User user = getCurrentUserOrThrow(userId);
        Organization org = getUserOrganizationOrThrow(user);

        Context context = contextRepository
                .findByIdAndOrganizationIdAndActiveTrue(contextId, org.getId())
                .orElseThrow(() -> new ContextNotFoundException("Context not found"));

        return ContextResponse.fromEntity(context);
    }

    // ---------- US-08: Update Context (name / optional fields) ----------

    @Transactional
    public ContextResponse updateContext(Long userId, Long contextId, UpdateContextRequest request) {

        if (contextId == null) {
            throw new ContextBadRequestException("Context id is required");
        }
        if (request == null) {
            throw new ContextBadRequestException("Request body cannot be null");
        }

        User user = getCurrentUserOrThrow(userId);
        Organization org = getUserOrganizationOrThrow(user);
        ensureOrganizationAdmin(user, org);

        Context context = contextRepository
                .findByIdAndOrganizationIdAndActiveTrue(contextId, org.getId())
                .orElseThrow(() -> new ContextNotFoundException("Context not found"));

        // Update name if provided and changed
        if (request.name() != null && !request.name().isBlank()) {
            String newName = request.name().trim();
            if (!newName.equalsIgnoreCase(context.getName())) {
                boolean exists = contextRepository.existsByOrganizationIdAndNameIgnoreCaseAndActiveTrue(
                        org.getId(), newName);
                if (exists) {
                    throw new ContextBadRequestException(
                            "A context with this name already exists in the organization");
                }
                context.setName(newName);
            }
        }

        // Optionally update type / description if provided
        if (request.type() != null) {
            context.setType(request.type());
        }
        if (request.description() != null) {
            context.setDescription(request.description());
        }

        Context saved = contextRepository.save(context);
        return ContextResponse.fromEntity(saved);
    }

    // ---------- US-08: Soft delete Context (optional) ----------

    // the context is changed to active = false

    @Transactional
    public void deleteContext(Long userId, Long contextId) {
        if (contextId == null) {
            throw new ContextBadRequestException("Context id is required");
        }

        User user = getCurrentUserOrThrow(userId);
        Organization org = getUserOrganizationOrThrow(user);
        ensureOrganizationAdmin(user, org);

        Context context = contextRepository
                .findByIdAndOrganizationIdAndActiveTrue(contextId, org.getId())
                .orElseThrow(() -> new ContextNotFoundException("Context not found"));

        context.setActive(false);
        contextRepository.save(context);
    }

    // ========================================================================
    // US-10
    // ========================================================================

    // ---------- US-10: promotes a context member to Context Admin ----------
    @Transactional
    public ContextAdminResponse promoteContextAdmin(Long actingUserId, Long contextId,
            PromoteContextAdminRequest request) {

        if (request == null || request.userId() == null) {
            throw new ContextBadRequestException("UserId is required");
        }

        User actingUser = getCurrentUserOrThrow(actingUserId);
        Context context = loadContextOrThrow(contextId);
        Organization org = context.getOrganization();

        // Only org admins can promote
        if (!isOrganizationAdmin(actingUser, org)) {
            throw new ContextForbiddenException("Only organization admins can promote context admins");
        }

        User targetUser = getCurrentUserOrThrow(request.userId());

        // Must belong to same organization
        if (!targetUser.getOrg().getId().equals(org.getId())) {
            throw new ContextBadRequestException("User does not belong to the organization");
        }

        // Must already be a member
        ContextMembership membership = membershipRepository
                .findByContextIdAndUserId(contextId, request.userId())
                .orElseThrow(() -> new ContextBadRequestException("User is not a member of this context"));

        // Promote
        membership.setContextAdmin(true);
        membershipRepository.save(membership);

        return ContextAdminResponse.fromMembership(membership);
    }

    // ------ US-10: demotes a Context Admin back to regular member ---------
    @Transactional
    public void demoteContextAdmin(Long actingUserId, Long contextId, Long targetUserId) {

        User actingUser = getCurrentUserOrThrow(actingUserId);
        Context context = loadContextOrThrow(contextId);
        Organization org = context.getOrganization();

        if (!isOrganizationAdmin(actingUser, org)) {
            throw new ContextForbiddenException("Only organization admins can demote context admins");
        }

        ContextMembership membership = membershipRepository
                .findByContextIdAndUserId(contextId, targetUserId)
                .orElseThrow(() -> new ContextBadRequestException("User is not a member of this context"));

        membership.setContextAdmin(false);
        membershipRepository.save(membership);
    }

    // -------- US-10: lists all current Context Admins for a specific context ---
    @Transactional(readOnly = true)
    public List<ContextAdminResponse> listContextAdmins(Long userId, Long contextId) {

        User actingUser = getCurrentUserOrThrow(userId);
        Context context = loadContextOrThrow(contextId);

        if (!canManageContextMembers(actingUser, context)) {
            throw new ContextForbiddenException(
                    "Only organization admins or context admins can view context admins");
        }

        List<ContextMembership> admins = membershipRepository.findByContextIdAndContextAdminTrue(contextId);

        return admins.stream()
                .map(ContextAdminResponse::fromMembership)
                .toList();
    }

    // ------- List current context for a especific context admin (for /login)

    @Transactional(readOnly = true)
    public List<ContextByAdminResponse> findContextByAdmin(User user) {

        List<ContextMembership> context = membershipRepository.findByUserIdAndContextAdminTrue(user.getId());

        return context.stream()
                .map(ContextByAdminResponse::fromMembership)
                .toList();
    }

    // ===============================================================
    // US-09
    // =================================================================

    // ----------US-09: accepts { userId } and adds the user to the context
    // if they are a member of the organization -----------------

    @Transactional
    public ContextMemberResponse addMember(Long actingUserId, Long contextId, AddContextMemberRequest request) {

        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new ContextBadRequestException("Email is required");
        }

        User actingUser = getCurrentUserOrThrow(actingUserId);
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found"));

        Organization org = context.getOrganization();

        if (!org.getId().equals(actingUser.getOrg().getId())) {
            throw new ContextForbiddenException("You do not belong to this organization");
        }

        ensureCanManageContextMembers(actingUser, context);

        User targetUser = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new ContextBadRequestException("User with this email does not exist"));

        if (targetUser.getOrg() == null || !targetUser.getOrg().getId().equals(org.getId())) {
            throw new ContextBadRequestException("User does not belong to this organization");
        }

        if (membershipRepository.existsByContextIdAndUserId(contextId, targetUser.getId())) {
            throw new ContextBadRequestException("User is already a member of this context");
        }

        ContextMembership membership = new ContextMembership();
        membership.setContext(context);
        membership.setUser(targetUser);
        membership.setUserEmail(targetUser.getEmail());
        membership.setContextAdmin(false);

        ContextMembership saved = membershipRepository.save(membership);
        return ContextMemberResponse.fromMembership(saved);
    }

    // ---------- US-09: removes the user from the context -----------------

    @Transactional
    public void removeMember(Long actingUserId, Long contextId, Long targetUserId) {

        // Context must exist
        User actingUser = getCurrentUserOrThrow(actingUserId);
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found"));

        Organization org = context.getOrganization();

        // User must belong to org
        if (!org.getId().equals(actingUser.getOrg().getId())) {
            throw new ContextForbiddenException("You do not belong to this organization");
        }

        // User must be an org or context admin
        ensureCanManageContextMembers(actingUser, context);

        // User to delete must be part of the context
        ContextMembership membership = membershipRepository
                .findByContextIdAndUserId(contextId, targetUserId)
                .orElseThrow(() -> new ContextBadRequestException("User is not a member of this context"));

        membershipRepository.delete(membership);
    }

    // ---------- US-09: list all of the members of a context -----------------

    @Transactional(readOnly = true)
    public List<ContextMemberResponse> listMembers(Long actingUserId, Long contextId) {

        User actingUser = getCurrentUserOrThrow(actingUserId);
        Context context = contextRepository.findById(contextId)
                .orElseThrow(() -> new ContextNotFoundException("Context not found"));

        // only org admins or context admins can list members
        if (!canManageContextMembers(actingUser, context)) {
            throw new ContextForbiddenException(
                    "Only organization admins or context admins can view context members");
        }

        return membershipRepository.findByContextId(contextId)
                .stream()
                .map(ContextMemberResponse::fromMembership)
                .toList();
    }

}
