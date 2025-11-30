package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.ContextRFCId;
import dsd.api.cdmsa.model.ContextReviewer;

@Repository
public interface ContextReviewerRepository extends JpaRepository<ContextReviewer, ContextRFCId> {
    
}
