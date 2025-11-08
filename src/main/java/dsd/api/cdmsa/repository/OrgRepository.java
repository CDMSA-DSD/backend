package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dsd.api.cdmsa.model.Organization;

public interface OrgRepository extends JpaRepository<Organization, Integer>{

    boolean existsByName (String email);
}

