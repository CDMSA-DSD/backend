package dsd.api.cdmsa.service;

import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionService {

     /*
    private final OrganizationRepository orgMemberRepo;

    public boolean canManageOrganization(Long orgId, User user) {
        return orgMemberRepo.existsByOrganizationIdAndUserIdAndRoleIn(
                orgId,
                user.getId(),
                List.of(OrgRole.OWNER, OrgRole.ADMIN)
        );
    }
    */
}
