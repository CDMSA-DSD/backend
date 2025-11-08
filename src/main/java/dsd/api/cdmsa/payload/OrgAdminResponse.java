package dsd.api.cdmsa.payload;

import dsd.api.cdmsa.model.Organization;
import dsd.api.cdmsa.model.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrgAdminResponse {

    private Organization org;
    private User admin;
}
