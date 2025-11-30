package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.UserRFCId;
import dsd.api.cdmsa.model.UserReviewer;

@Repository
public interface UserReviewerRepository extends JpaRepository<UserReviewer, UserRFCId> {
    
}
