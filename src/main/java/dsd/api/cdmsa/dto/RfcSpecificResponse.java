package dsd.api.cdmsa.dto;

import java.util.List;

import dsd.api.cdmsa.model.RFC;

public record RfcSpecificResponse(
        Long id,
        String title,
        String description,
        Long userId,
        String authorName,
        Long templateId,
        Long orgId,
        RFC.Status status,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        boolean isAuthor,
        List<AlternativeResponse> alternatives,
        List<CommentResponse> comments) {

}
