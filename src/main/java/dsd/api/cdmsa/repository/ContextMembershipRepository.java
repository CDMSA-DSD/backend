package dsd.api.cdmsa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.ContextMembership;

@Repository
public interface ContextMembershipRepository extends JpaRepository<ContextMembership, Long> {

    // Returns the membership of a given user in a given context.
    Optional<ContextMembership> findByContextIdAndUserId(Long contextId, Long userId);

    // Returns the context of a given user
    List<ContextMembership> findByUserId(Long userId);

    // Returns all memberships of a context (members).
    List<ContextMembership> findByContextId(Long contextId);

    // Returns all context admins within a context.
    List<ContextMembership> findByContextIdAndContextAdminTrue(Long contextId);

    // Returns all context admins within a context.
    List<ContextMembership> findByUserIdAndContextAdminTrue(Long userId);

    // Checks if a user is already a member of a context.
    boolean existsByContextIdAndUserId(Long contextId, Long userId);

    // Checks if a user is already a member of a context and is a context admin.
    boolean existsByContextIdAndUserIdAndContextAdminTrue(Long contextId, Long userId);

    

}
