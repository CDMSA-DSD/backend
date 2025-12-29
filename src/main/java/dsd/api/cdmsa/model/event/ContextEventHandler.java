package dsd.api.cdmsa.model.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dsd.api.cdmsa.model.Notification;
import dsd.api.cdmsa.model.Notification.NotificationType;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.NotificationRepository;
import dsd.api.cdmsa.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextEventHandler {

    private final UserService userService;
    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContextAdminPromotedEvent event) {
        log.info("AFTER_COMMIT -> Promotion Context Admin contextId={}, targetUserId={}",
                event.contextId(), event.targetUserId());

        // Getting the promoted user
        User targetUser = userService.searchById(event.targetUserId());

        if (targetUser.getEmail().equalsIgnoreCase(event.actingAdminEmail())) return;

        Notification noti = new Notification();
        noti.setTargetUser(targetUser);
        noti.setMessage("You have been promoted ADMIN for the context: " + event.contextName());
        noti.setDetails("by: " + event.actingAdminEmail());

        noti.setType(NotificationType.PROMOTION);

        noti.setCreatedAt(java.time.Instant.now());
        noti.setRead(false);

        notificationRepository.save(noti);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserAddedToContextEvent event) {
        log.info("AFTER_COMMIT -> User Added to Context: contextId={}, targetUserId={}",
                event.contextId(), event.targetUserId());

        User targetUser = userService.searchById(event.targetUserId());

        if (targetUser.getEmail().equalsIgnoreCase(event.actingAdminEmail())) return;

        Notification noti = new Notification();
        noti.setTargetUser(targetUser);


        noti.setMessage("You have been added to the context: " + event.contextName());
        noti.setDetails("Added by: " + event.actingAdminEmail() +
                ". You can now participate in RFCs and decisions for this team/project.");

        noti.setType(NotificationType.CONTEXT_ASSIGNMENT);

        noti.setCreatedAt(java.time.Instant.now());
        noti.setRead(false);

        notificationRepository.save(noti);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContextAdminDemotedEvent event) {
        log.info("AFTER_COMMIT -> Context Admin Demoted: contextId={}, targetUserId={}",
                event.contextId(), event.targetUserId());

        User targetUser = userService.searchById(event.targetUserId());

        if (targetUser.getEmail().equalsIgnoreCase(event.actingAdminEmail())) return;

        Notification noti = new Notification();
        noti.setTargetUser(targetUser);

        noti.setType(NotificationType.PROMOTION);

        noti.setMessage("You are not more the ADMIN for the context: " + event.contextName());
        noti.setDetails("by: " + event.actingAdminEmail());

        noti.setCreatedAt(java.time.Instant.now());
        noti.setRead(false);

        notificationRepository.save(noti);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserRemovedFromContextEvent event) {
        log.info("AFTER_COMMIT -> User Removed from Context: contextId={}, targetUserId={}",
                event.contextId(), event.targetUserId());

        User targetUser = userService.searchById(event.targetUserId());

        if (targetUser.getEmail().equalsIgnoreCase(event.actingAdminEmail())) return;

        Notification noti = new Notification();
        noti.setTargetUser(targetUser);

        noti.setType(NotificationType.CONTEXT_ASSIGNMENT);

        noti.setMessage("You have been removed from the context: " + event.contextName());
        noti.setDetails("by: " + event.actingAdminEmail());

        noti.setCreatedAt(java.time.Instant.now());
        noti.setRead(false);

        notificationRepository.save(noti);
    }
}