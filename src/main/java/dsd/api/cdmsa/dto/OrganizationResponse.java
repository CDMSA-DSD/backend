package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationResponse(
        Long id,
        @NotBlank(message = "Username cannot be blank") String companyName,
        String description,
        String domain,
        String selectedRepoName,
        String selectedBranchName,
        String repoOwner
        ) {
}
