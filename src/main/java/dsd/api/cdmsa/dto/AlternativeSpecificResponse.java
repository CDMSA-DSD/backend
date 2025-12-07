package dsd.api.cdmsa.dto;

import java.util.List;

public record AlternativeSpecificResponse(
        Long id,
        String title,
        String description,
        String pros,
        String cons,
        String xml,
        Long authorId,
        String authorName,
        String addition,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        int yes,
        int no,
        boolean isAuthor,
        List<AttachmentResponse> attachments
) {}
