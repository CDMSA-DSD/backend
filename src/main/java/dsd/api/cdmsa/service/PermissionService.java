package dsd.api.cdmsa.service;

import org.springframework.stereotype.Service;

import dsd.api.cdmsa.model.Alternative;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.repository.AlternativeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserService userService;
    private final ContextService contextService;
    private final RfcService rfcService;
    private final AlternativeRepository alternativeRepository;

    // === ORG PERMISSIONS ===

    public boolean canManageOrg(UserPrincipal principal) {
        return userService.isOrgAdmin(principal.getUser());
    }

    // === CONTEXT PERMISSIONS ===

    public boolean canManageContext(UserPrincipal principal, Long contextId) {
        return contextService.isContextAdmin(principal.getUser(), contextId) ||
                canManageOrg(principal);
    }

    // === AUTHOR PERMISSIONS ===
    public boolean canManageRfc(UserPrincipal principal, Long rfcId) {
        return rfcService.isAuthor(principal.getUser(),rfcId) ||
                canManageOrg(principal);
    }

    // === REVIEWER PERMISSION ===
    public boolean canReviewRfc(UserPrincipal principal, Long rfcId) {
        return rfcService.isReviewer(principal.getUser(),rfcId) ||
                canManageRfc(principal,rfcId);
    }

    // == VOTER PERMISSION ===
    public boolean canVoteRfc(UserPrincipal principal, Long altId) {
        Alternative alternative = alternativeRepository.findById(altId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative not found"));
        
        Long rfcId = alternative.getRfc().getId();

        return canReviewRfc(principal, rfcId);
    }

}
