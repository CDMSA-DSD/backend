package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationResponse(
        @NotBlank(message = "Username cannot be blank") String companyName,
        String description,
        String domain,
        boolean isEmailDomainRequired,
        String selectedRepoName,
        String selectedBranchName,
        String repoOwner
        ) {
}
