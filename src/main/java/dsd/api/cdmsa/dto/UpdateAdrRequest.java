package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ADR;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO used to update an existing ADR.
 * Contains the fields that are allowed to be changed.
 */
public record UpdateAdrRequest(
        @NotBlank String title,
        @NotBlank String context,
        @NotBlank String decision,
        String consequences,
        @NotNull ADR.Status status
) {
}
