package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrganizationRequest(
        @NotBlank String companyName,
        @NotBlank String description,
        @NotBlank String domain,
        String selectedRepoName,
        String selectedBranchName
        ) {
}
