package dsd.api.cdmsa.mapper;

import dsd.api.cdmsa.dto.OrganizationResponseAlt;
import dsd.api.cdmsa.model.Organization;

public class OrgMapper {

    // Map Org entity to OrgDto without admin
    public static OrganizationResponseAlt toDto(Organization org) {
        return new OrganizationResponseAlt(
                org.getId(),
                org.getName(),
                org.getDescription(),
                org.getDomain());
    }
}
