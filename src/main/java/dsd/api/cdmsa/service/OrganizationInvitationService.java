package dsd.api.cdmsa.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import dsd.api.cdmsa.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import dsd.api.cdmsa.dto.LinkResponse;
import dsd.api.cdmsa.exception.InvitationExpiredException;
import dsd.api.cdmsa.exception.InvitationInvalidException;
import dsd.api.cdmsa.exception.InvitationNotFoundException;
import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.OrganizationInvitation;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import dsd.api.cdmsa.repository.OrganizationInvitationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationInvitationService {

    private final OrganizationInvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public LinkResponse createGeneralInvitationLink(UserPrincipal authUser) {

        User user = authUser.getUser();
        Organization org = user.getOrg();

        String token = generateSecureToken();

        OrganizationInvitation invitation = new OrganizationInvitation();
        invitation.setOrg(org);
        invitation.setCreatedBy(user);
        invitation.setEmail(null);
        invitation.setToken(token);
        invitation.setState(OrganizationInvitation.State.ACTIVE);
        invitation.setCreatedAt(Instant.now());
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        invitationRepository.save(invitation);

        return new LinkResponse(
                invitation.getId(),
                frontendBaseUrl + "/accept-invitation?token=" + token,
                invitation.getExpiresAt(),
                invitation.getState());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public OrganizationInvitation getInvitation(Long invitationId) {
        OrganizationInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        return invitation;
    }

    public OrganizationInvitation getInvitationByToken(String token) {
        OrganizationInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationInvalidException());

        if (isInvitationExpired(invitation)) {
            throw new InvitationExpiredException();
        }

        return invitation;
    }

    private boolean isInvitationExpired(OrganizationInvitation invitation) {
        return invitation.getExpiresAt().isBefore(Instant.now());
    }

    public Page<OrganizationInvitation> findAllInvitations(Long orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return invitationRepository.findByOrgId(orgId, pageable);
    }

    @Transactional
    public void deleteInvitation(Long invitationId, UserPrincipal principal) {
        OrganizationInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));

        Organization org = organizationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));

        if (!invitation.getOrg().getId().equals(principal.getOrgId())) {
            throw new AccessDeniedException("You cannot handle this organization's invitations");
        }

        if (!principal.getUser().getId().equals(org.getAdminUser().getId())) {
            throw new AccessDeniedException("Only the admin can delete the organization's invitations");
        }

        // used a soft deletion (can be changed with really deletion from the db)
        invitation.setState(OrganizationInvitation.State.DISABLED);
        invitationRepository.save(invitation);
    }
}