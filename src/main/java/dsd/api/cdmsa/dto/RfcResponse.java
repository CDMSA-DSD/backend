package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.RFC;

public record RfcResponse(
        Long id,
        String title,
        String description,
        Long userId,
        Long templateId,
        Long orgId,
        RFC.Status status) {
}