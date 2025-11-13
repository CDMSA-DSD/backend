package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record RepoBranchSelectionRequest(
        @NotBlank String repoName,
        @NotBlank String branchName
) {}
