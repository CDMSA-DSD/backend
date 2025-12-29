package dsd.api.cdmsa.model.event;

import dsd.api.cdmsa.model.Notification;
import dsd.api.cdmsa.model.Notification.NotificationType;
import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.repository.NotificationRepository;
import dsd.api.cdmsa.service.ContextService;
import dsd.api.cdmsa.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RfcEventHandler {

    private final UserService userService;
    private final ContextService contextService;
    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewerAssignment(ReviewersAssignedEvent event) {
        log.info("AFTER_COMMIT -> Reviewer assignment for RFC ID: {}", event.rfcId());

        Set<Long> usersToNotify = new HashSet<>();

        if (event.userIds() != null) {
            usersToNotify.addAll(event.userIds());
        }

        if (event.contextIds() != null) {
            for (Long contextId : event.contextIds()) {
                contextService.getMembersForNotification(contextId).forEach(member ->
                        usersToNotify.add(member.userId())
                );
            }
        }

        if (usersToNotify.isEmpty()) {
            log.warn("No user found for the RFC notification: {}", event.rfcId());
            return;
        }

        List<User> targetUsers = userService.findAllUsersByid(new ArrayList<>(usersToNotify), event.orgId());

        log.info("DEBUG: Found {} users in the DB", targetUsers.size());

        List<Notification> notifications = targetUsers.stream()
                // filter to not notify the author
                .filter(user -> !user.getEmail().equalsIgnoreCase(event.authorEmail()))
                .map(user -> {
                    Notification n = new Notification();
                    n.setTargetUser(user);
                    n.setRfcId(event.rfcId());
                    n.setType(NotificationType.REVIEW_ASSIGNMENT);
                    n.setMessage("You have been assigned as REVIEWER for the RFC: " + event.rfcTitle());
                    n.setDetails("by: " + event.authorEmail());
                    n.setRead(false);
                    return n;
                }).toList();

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("SUCCESS: {} notification saved (author {} excluded)",
                    notifications.size(), event.authorEmail());
        } else {
            log.info("No notification saved, list was empty after filtering out the author");
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewerRemoval(ReviewersRemovedEvent event) {
        log.info("AFTER_COMMIT -> Handling removal for RFC ID: {}", event.rfcId());

        Set<Long> usersToNotify = new HashSet<>();
        if (event.userIds() != null) usersToNotify.addAll(event.userIds());
        if (event.contextIds() != null) {
            for (Long contextId : event.contextIds()) {
                contextService.getMembersForNotification(contextId).forEach(m -> usersToNotify.add(m.userId()));
            }
        }

        if (usersToNotify.isEmpty()) return;

        List<User> targetUsers = userService.findAllUsersByid(new ArrayList<>(usersToNotify), event.orgId());

        List<Notification> notifications = targetUsers.stream()
                .map(user -> {
                    Notification n = new Notification();
                    n.setTargetUser(user);
                    n.setRfcId(event.rfcId());
                    n.setType(NotificationType.REVIEW_ASSIGNMENT); // Puoi aggiungere REVIEW_REMOVED all'Enum se preferisci
                    n.setMessage("Assignment Revoked: RFC " + event.rfcTitle());
                    n.setDetails("You are no longer a reviewer for this RFC. Revoked by: " + event.authorEmail());
                    n.setRead(false);
                    return n;
                }).toList();

        notificationRepository.saveAll(notifications);
    }
}