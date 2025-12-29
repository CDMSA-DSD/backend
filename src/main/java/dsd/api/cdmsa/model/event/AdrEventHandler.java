package dsd.api.cdmsa.model.event;

import dsd.api.cdmsa.model.Notification;
import dsd.api.cdmsa.model.Notification.NotificationType;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.NotificationRepository;
import dsd.api.cdmsa.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdrEventHandler {

    private final UserService userService;
    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AdrPublishedEvent event) {
        log.info("AFTER_COMMIT -> ADR Published: adrId={}, notifyCount={}",
                event.adrId(), event.targetUserIds().size());

        if (event.targetUserIds().isEmpty()) {
            return;
        }

        List<User> targetUsers = userService.findAllUsersByid(event.targetUserIds(), event.orgId());

        log.info("DEBUG: Found {} users for the ADR notification", targetUsers.size());

        List<Notification> notifications = targetUsers.stream()
                .map(user -> {
                    Notification n = new Notification();
                    n.setTargetUser(user);
                    n.setRfcId(event.rfcId());
                    n.setType(NotificationType.ADR_PUBLISHED);
                    n.setMessage("Decision Published: " + event.adrTitle());
                    n.setDetails("The Architecture Decision Record has been officially published and synced to GitHub.");
                    n.setRead(false);
                    return n;
                }).toList();

        notificationRepository.saveAll(notifications);
    }
}