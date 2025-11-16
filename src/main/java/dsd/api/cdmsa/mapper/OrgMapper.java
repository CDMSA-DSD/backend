package dsd.api.cdmsa.mapper;

import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.model.Organization;

public class OrgMapper {

    // Map Org entity to OrgDto without admin
    public static OrganizationResponse toDto(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getDescription(),
                org.getDomain());
    }
}
