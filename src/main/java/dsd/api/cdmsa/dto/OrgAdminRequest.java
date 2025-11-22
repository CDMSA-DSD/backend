package dsd.api.cdmsa.dto;

public record OrgAdminRequest(
        // Org data
        OrganizationResponse org,
        // Admin data   
        SignInRequest admin
        ) {}