package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.Size;

public record UpdateAlternativeRequest(
        @Size(max = 150, message = "Title too long")
        String title,

        @Size(max = 4000, message = "Description too long")
        String description,

        @Size(max = 2000, message = "Pros text too long")
        String pros,

        @Size(max = 2000, message = "Cons text too long")
        String cons,
        String addition
) {}