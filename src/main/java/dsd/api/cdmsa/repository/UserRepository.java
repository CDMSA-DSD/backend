package dsd.api.cdmsa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dsd.api.cdmsa.model.User;

public interface UserRepository extends JpaRepository<User, Integer>{

    boolean existsByEmail (String email);
    Optional<User> findByUsername (String username);
}

