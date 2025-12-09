package dsd.api.cdmsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.ContextRFCId;
import dsd.api.cdmsa.model.ContextReviewer;
import dsd.api.cdmsa.model.RFC;

@Repository
public interface ContextReviewerRepository extends JpaRepository<ContextReviewer, ContextRFCId> {
    List<ContextReviewer> findAllByRfc(RFC rfc);
    
}
