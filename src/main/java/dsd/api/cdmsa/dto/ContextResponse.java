package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.Context;

public record ContextResponse(
        Long id,
        Long organizationId,
        String name,
        String type,
        String description) {

    public static ContextResponse fromEntity(Context context) {
        return new ContextResponse(
                context.getId(),
                context.getOrganization() != null ? context.getOrganization().getId() : null,
                context.getName(),
                context.getType(),
                context.getDescription());
    }
}
