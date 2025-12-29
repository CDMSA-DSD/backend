package dsd.api.cdmsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.model.UserRFCId;
import dsd.api.cdmsa.model.UserReviewer;

@Repository
public interface UserReviewerRepository extends JpaRepository<UserReviewer, UserRFCId> {
    List<UserReviewer> findAllByRfc(RFC rfc);
    List<UserReviewer> findAllByRfcId(Long rfcId);
    void deleteByRfcId(Long rfcId);
    
}
