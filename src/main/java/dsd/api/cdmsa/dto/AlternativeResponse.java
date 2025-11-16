package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.Alternative;

public record AlternativeResponse(
        Long id,
        String title,
        String description,
        String pros,
        String cons,
        Long authorId,
        String authorName,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        int yes,
        int no) {

    public static AlternativeResponse fromEntity(Alternative alternative) {
        return new AlternativeResponse(
                alternative.getId(),
                alternative.getTitle(),
                alternative.getDescription(),
                alternative.getPros(),
                alternative.getCons(),
                alternative.getAuthor() != null ? alternative.getAuthor().getId() : null,
                alternative.getAuthor() != null ? alternative.getAuthor().getName() : null,
                alternative.getCreatedAt(),
                alternative.getUpdatedAt(),
                0,
                0);
    }

    public static AlternativeResponse fromEntityVotes(Alternative alternative, int yes, int no) {
        return new AlternativeResponse(
                alternative.getId(),
                alternative.getTitle(),
                alternative.getDescription(),
                alternative.getPros(),
                alternative.getCons(),
                alternative.getAuthor() != null ? alternative.getAuthor().getId() : null,
                alternative.getAuthor() != null ? alternative.getAuthor().getName() : null,
                alternative.getCreatedAt(),
                alternative.getUpdatedAt(), yes, no);
    }
}