package dsd.api.cdmsa.dto;

public record CreateRfcRequest(
        String title,
        String description,
        Long templateId,
        String xml  // if null no diagram has been created (yet)
        ) {
}
