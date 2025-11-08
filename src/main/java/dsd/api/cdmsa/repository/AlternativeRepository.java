package dsd.api.cdmsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import dsd.api.cdmsa.model.Alternative;

@Repository
public interface AlternativeRepository extends JpaRepository<Alternative, Long> {
    List<Alternative> findByRfcId(Long rfcId);
}
