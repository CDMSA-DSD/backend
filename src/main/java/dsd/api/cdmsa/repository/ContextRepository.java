package dsd.api.cdmsa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.Context;

@Repository
public interface ContextRepository extends JpaRepository<Context, Long> {

    // Returns all active contexts for a given organization.
    List<Context> findByOrganizationIdAndActiveTrue(Long organizationId);

    // Returns an active context by id and organization.
    Optional<Context> findByIdAndOrganizationIdAndActiveTrue(Long id, Long organizationId);

    // Checks if there is another active context with the same name in the same
    // organization.
    boolean existsByOrganizationIdAndNameIgnoreCaseAndActiveTrue(Long organizationId, String name);
}
