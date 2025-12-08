package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAlternativeRequest(
                @NotBlank String title,
                @NotBlank String description,
                String pros,
                String cons,
                String xml) {
}
