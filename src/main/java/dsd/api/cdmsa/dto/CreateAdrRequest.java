package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ADR;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAdrRequest(
        @NotBlank String title,
        @NotBlank String context,
        @NotBlank String decision,
        @NotBlank String consequences,
        ADR.Status status,
        @NotNull Long rfcId) {
}