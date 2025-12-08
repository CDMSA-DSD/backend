package dsd.api.cdmsa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dsd.api.cdmsa.model.Notification;
import dsd.api.cdmsa.model.User;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByTargetUserOrderByCreatedAtDesc(User targetUser, Pageable pagable);

    Page<Notification> findByTargetUserAndReadOrderByCreatedAtDesc(User targetUser, boolean read, Pageable pageable);

    boolean existsByTargetUserAndReadFalse(User targetUser);

    long countByTargetUserAndReadFalse(User targetUser);

    @Modifying
    @Query ("""
            UPDATE Notification n
            SET n.read = true
            WHERE n.id = :id AND n.targetUser = :targetUser
            """)
    int markAsRead(@Param("targetUser") User targetUser, @Param("id") Long id);

    @Modifying
    @Query ("""
            UPDATE Notification n
            SET n.read = true
            WHERE n.read = false AND n.targetUser = :targetUser
            """)
    int markAllAsRead(@Param("targetUser") User targetUser);

}
