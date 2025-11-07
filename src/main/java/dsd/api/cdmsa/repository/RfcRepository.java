package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dsd.api.cdmsa.model.RFC;

public interface RfcRepository extends JpaRepository<RFC, Long> {
}
