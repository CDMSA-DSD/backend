package dsd.api.cdmsa.dto;

public class OrganizationRequest {

    private String companyName;

    private String description;
    private String domain;
    private String gitHubToken;

    // getter e setter
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getGitHubToken() { return gitHubToken; }
    public void setGitHubToken(String gitHubToken) { this.gitHubToken = gitHubToken; }
}
