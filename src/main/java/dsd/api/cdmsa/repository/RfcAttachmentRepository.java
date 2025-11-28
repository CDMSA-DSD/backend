package dsd.api.cdmsa.repository;

import dsd.api.cdmsa.model.RfcAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RfcAttachmentRepository extends JpaRepository<RfcAttachment, Long> {
    List<RfcAttachment> findByRfcId(Long rfcId);
}
