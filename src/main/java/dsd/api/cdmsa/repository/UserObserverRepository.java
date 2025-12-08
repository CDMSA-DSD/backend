package dsd.api.cdmsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.Observer;
import dsd.api.cdmsa.model.UserRFCId;

@Repository
public interface UserObserverRepository extends JpaRepository<Observer, UserRFCId> {
}
