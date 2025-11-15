package dsd.api.cdmsa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dsd.api.cdmsa.dto.ContextResponse;
import dsd.api.cdmsa.dto.CreateContextRequest;
import dsd.api.cdmsa.dto.UpdateContextRequest;
import dsd.api.cdmsa.exception.ContextBadRequestException;
import dsd.api.cdmsa.exception.ContextForbiddenException;
import dsd.api.cdmsa.exception.ContextNotFoundException;
import dsd.api.cdmsa.model.Context;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.ContextRepository;
import dsd.api.cdmsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContextService {

    private final ContextRepository contextRepository;
    private final UserRepository userRepository;

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
}
