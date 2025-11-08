package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.Template;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
}
