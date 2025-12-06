package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRfcRequest(
        @NotBlank(message = "Title cannot be empty")
        String title,

        @NotBlank(message = "Description cannot be empty")
        String description,
        String addition
) {}
