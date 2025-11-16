package dsd.api.cdmsa.repository;


import dsd.api.cdmsa.model.ADR;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdrRepository extends JpaRepository<ADR, Long> {
    Optional<ADR> findByRfcId(Long rfcId);
}
