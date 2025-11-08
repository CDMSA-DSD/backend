package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.Comment;

public record CommentResponse(
        Long id,
        String content,
        Long authorId,
        String authorName,
        java.time.Instant createdAt,
        java.time.Instant updatedAt) {

    public static CommentResponse fromEntity(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getContent(),
                c.getAuthor() != null ? c.getAuthor().getId() : null,
                c.getAuthor() != null ? c.getAuthor().getName() : null,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}