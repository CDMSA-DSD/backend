package dsd.api.cdmsa.repository;

import dsd.api.cdmsa.model.AlternativeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlternativeAttachmentRepository extends JpaRepository<AlternativeAttachment, Long> {
    List<AlternativeAttachment> findByAlternativeId(Long alternativeId);
}