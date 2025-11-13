package dsd.api.cdmsa.dto;

public record OrganizationResponse(
        String companyName,
        String description,
        String domain,
        String selectedRepoName,
        String selectedBranchName,
        String repoOwner
        ) {
}
