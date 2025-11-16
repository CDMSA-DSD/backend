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
        java.time.Instant updatedAt) {

    public static AlternativeResponse fromEntity(Alternative alternative) {
        return new AlternativeResponse(
                alternative.getId(),
                alternative.getTitle(),
                alternative.getDescription(),
                alternative.getPros(),
                alternative.getCons(),
                alternative.getAuthor() != null ? alternative.getAuthor().getId() : null,
                alternative.getAuthor() != null ? alternative.getAuthor().getFirstname() : null,
                alternative.getCreatedAt(),
                alternative.getUpdatedAt());
    }
}