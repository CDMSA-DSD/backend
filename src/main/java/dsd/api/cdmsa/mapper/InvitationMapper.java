package dsd.api.cdmsa.mapper;

import dsd.api.cdmsa.dto.InvitationResponse;
import dsd.api.cdmsa.model.OrganizationInvitation;

public class InvitationMapper {
    // Map Invitation entity to InvitationDto without author and org
    public static InvitationResponse toDto(OrganizationInvitation invitation) {
        return new InvitationResponse(
            invitation.getId(),
            invitation.getToken(),
            invitation.getState(),
            invitation.getCreatedAt(),
            invitation.getExpiresAt()
        );
    }
}
