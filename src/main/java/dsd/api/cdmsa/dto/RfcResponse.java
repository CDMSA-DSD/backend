package dsd.api.cdmsa.dto;

import java.util.List;

import dsd.api.cdmsa.model.RFC;

public record RfcResponse(
                Long id,
                String title,
                String description,
                Long userId,
                String authorName,
                Long templateId,
                Long orgId,
                RFC.Status status,
                String addition,
                java.time.Instant createdAt,
                java.time.Instant updatedAt,
                String xml,
                Long commentCount,
                List<AlternativeResponse> alternatives,
                List<CommentResponse> comments,
                List<AttachmentResponse> attachments) {
}
