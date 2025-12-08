package dsd.api.cdmsa.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import dsd.api.cdmsa.exception.NotificationNotFoundException;
import dsd.api.cdmsa.model.Notification;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;

    public Page<Notification> findAllNotificationByUserId(User user, Boolean read, int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        if (read == null) {
            return notificationRepository.findByTargetUserOrderByCreatedAtDesc(user, pageable);       
        }

        return notificationRepository.findByTargetUserAndReadOrderByCreatedAtDesc(user, read, pageable); 
    }

    public boolean areThereNotReadNotis(User user){
        return notificationRepository.existsByTargetUserAndReadFalse(user);
    }

    public long countNotReadNotis(User user) {
        return notificationRepository.countByTargetUserAndReadFalse(user);
    }

    @Transactional
    public void markNotiAsRead(User user, Long notiId){
        int n = notificationRepository.markAsRead(user, notiId);
        if (n == 0) {
            throw new NotificationNotFoundException(notiId);
        }
    }

    @Transactional
    public void markAllNotisAsRead(User user){
        notificationRepository.markAllAsRead(user);
    }
}
