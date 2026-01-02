package dsd.api.cdmsa.dto;

public record OrgAdminResponse(
        Long orgId,
        LoginResponse admin) {
}
