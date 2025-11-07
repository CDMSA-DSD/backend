package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.Alternative;

public record AlternativeResponse(
        Long id,
        // Long rfcId,
        String title,
        String description,
        String pros,
        String cons,
        Long authorId) {

    public static AlternativeResponse fromEntity(Alternative alternative) {
        return new AlternativeResponse(
                alternative.getId(),
                // alternative.getRfc().getId(),
                alternative.getTitle(),
                alternative.getDescription(),
                alternative.getPros(),
                alternative.getCons(),
                alternative.getAuthor() != null ? alternative.getAuthor().getId() : null);
    }
}