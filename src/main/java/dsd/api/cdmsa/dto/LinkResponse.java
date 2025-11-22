package dsd.api.cdmsa.dto;

import java.time.Instant;

import dsd.api.cdmsa.model.OrganizationInvitation.State;

public record LinkResponse(
    Long id,
    String link,
    Instant expiresAt,
    State state
) {} 