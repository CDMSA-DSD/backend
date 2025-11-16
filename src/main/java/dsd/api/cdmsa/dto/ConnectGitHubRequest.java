package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record ConnectGitHubRequest(
        @NotBlank String pat
) {}
