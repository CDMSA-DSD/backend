package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.Context;

public record ContextSummaryResponse(
        Long id,
        String name) {

    public static ContextSummaryResponse fromEntity(Context context) {
        return new ContextSummaryResponse(
                context.getId(),
                context.getName());
    }
}
