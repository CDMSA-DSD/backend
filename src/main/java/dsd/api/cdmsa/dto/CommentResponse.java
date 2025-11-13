package dsd.api.cdmsa.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long authorId,
        String author,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static CommentResponse fromEntity(dsd.api.cdmsa.model.Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getAuthor() != null ? c.getAuthor().getId() : null,
                c.getAuthor() != null ? c.getAuthor().getUsername() : null,
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
