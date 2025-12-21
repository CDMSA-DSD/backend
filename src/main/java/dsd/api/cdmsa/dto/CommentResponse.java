package dsd.api.cdmsa.dto;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
        Long id,
        Long authorId,
        String author,
        String authorName,
        String content,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> replies) {

    public static CommentResponse fromEntity(dsd.api.cdmsa.model.Comment c, List<CommentResponse> replies) {
        return new CommentResponse(
                c.getId(),
                c.getAuthor() != null ? c.getAuthor().getId() : null,
                c.getAuthor() != null ? c.getAuthor().getEmail() : null,
                c.getAuthor() != null ? c.getAuthor().getFirstname() + " " + c.getAuthor().getLastname() : null,
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                replies);
    }
}
