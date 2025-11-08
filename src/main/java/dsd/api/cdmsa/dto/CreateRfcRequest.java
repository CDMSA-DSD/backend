package dsd.api.cdmsa.dto;

public record CreateRfcRequest(
        String title,
        String description,
        Long templateId,
        Long orgId) {
}
