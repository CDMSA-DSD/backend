package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 50)
        String firstname,

        @Size(max = 50)
        String lastname,

        @Size(max = 50)
        String jobTitle
) {}