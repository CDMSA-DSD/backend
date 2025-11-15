package dsd.api.cdmsa.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgAdminRequest {

    // Org data
    private String orgName;
    private String orgDomain;
    private String orgDescription;

    // Admin data
    private String adminName;
    private String adminUsername;
    private String adminEmail;
    private String adminPassword;
}
