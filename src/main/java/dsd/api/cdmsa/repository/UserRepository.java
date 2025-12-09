package dsd.api.cdmsa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dsd.api.cdmsa.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Page<User> findByOrgId(Long orgId, Pageable pageable);

    List<User> findByIdInAndOrgId(List<Long> ids, Long orgId);
}
