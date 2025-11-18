package dsd.api.cdmsa.repository;


import dsd.api.cdmsa.model.ADR;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdrRepository extends JpaRepository<ADR, Long> {
	// Find ADRs whose RFC belongs to the given organization (paginated)
	org.springframework.data.domain.Page<ADR> findByRfc_Org_Id(Long orgId, org.springframework.data.domain.Pageable pageable);

	// Find an ADR by id only if its RFC belongs to the given organization
	java.util.Optional<ADR> findByIdAndRfc_Org_Id(Long id, Long orgId);

	// Check existence constrained to organization
	boolean existsByIdAndRfc_Org_Id(Long id, Long orgId);
}
