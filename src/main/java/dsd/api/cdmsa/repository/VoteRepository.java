package dsd.api.cdmsa.repository;

import dsd.api.cdmsa.model.Alternative;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    // Find user's vote on a specific alternative
    Optional<Vote> findByAlternativeAndVoter(Alternative alternative, User voter);

    // Counts # of yes or no of an alternative
    int countByAlternativeAndOutcome(Alternative alternative, boolean outcome);
}

