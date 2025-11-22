package dsd.api.cdmsa.dto;

import java.time.Instant;

import dsd.api.cdmsa.model.OrganizationInvitation.State;

public record InvitationResponse(
    Long id,
    String token,
    State state,
    Instant createdAt,
    Instant expiresAt
) {}
