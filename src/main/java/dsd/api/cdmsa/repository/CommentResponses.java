package dsd.api.cdmsa.repository;

import dsd.api.cdmsa.model.Comment;

public record CommentResponses(
        Long id,
        String content,
        Long authorId,
        String authorName,
        java.time.Instant createdAt,
        java.time.Instant updatedAt) {

    public static CommentResponses fromEntity(Comment c) {
        return new CommentResponses(
                c.getId(),
                c.getContent(),
                c.getAuthor() != null ? c.getAuthor().getId() : null,
                c.getAuthor() != null ? c.getAuthor().getName() : null,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}