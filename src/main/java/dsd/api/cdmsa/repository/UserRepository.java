package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}