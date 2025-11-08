package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
