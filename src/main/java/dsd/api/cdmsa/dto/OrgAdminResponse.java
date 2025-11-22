package dsd.api.cdmsa.dto;

import java.util.Map;

public record OrgAdminResponse(
        Long id_org,
        Map<String, Object> adminUri) {
}
