package dsd.api.cdmsa.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.OrganizationInvitation;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, Long> {

    Page<OrganizationInvitation> findByOrgId(Long orgId, Pageable pageable);

    Optional<OrganizationInvitation> findByToken(String token);
}
