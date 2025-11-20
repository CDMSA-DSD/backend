package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dsd.api.cdmsa.model.RFC;

public interface RfcRepository extends JpaRepository<RFC, Long> {
	// Find RFCs that belong to a specific organization (paginated)
	org.springframework.data.domain.Page<RFC> findByOrgId(Long orgId, org.springframework.data.domain.Pageable pageable);

	// Find a RFC by id only if it belongs to the given organization
	java.util.Optional<RFC> findByIdAndOrgId(Long id, Long orgId);

	// Check existence constrained to organization
	boolean existsByIdAndOrgId(Long id, Long orgId);
}
