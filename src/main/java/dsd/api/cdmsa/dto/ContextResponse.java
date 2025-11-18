package dsd.api.cdmsa.dto;

import java.time.Instant;

import dsd.api.cdmsa.model.Context;

public record ContextResponse(
        Long id,
        Long organizationId,
        String name,
        String type,
        String description,
        Instant createdAt) {

    public static ContextResponse fromEntity(Context context) {
        return new ContextResponse(
                context.getId(),
                context.getOrganization() != null ? context.getOrganization().getId() : null,
                context.getName(),
                context.getType(),
                context.getDescription(),
                context.getCreatedAt());
    }
}
