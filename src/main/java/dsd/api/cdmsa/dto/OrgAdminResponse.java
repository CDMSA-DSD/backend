package dsd.api.cdmsa.dto;

import java.util.Map;

public record OrgAdminResponse(
        Long orgId,
        Map<String, Object> adminUri) {
}
