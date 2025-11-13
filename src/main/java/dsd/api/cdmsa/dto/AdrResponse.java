package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ADR;

public record AdrResponse(
        Long id,
        String title,
        String context,
        String decision,
        String consequences,
        ADR.Status status,
        Long rfcId,
        java.time.Instant createdAt,
        java.time.Instant updatedAt) {
}
