package dsd.api.cdmsa.model.event;

import java.util.List;

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

@Slf4j // create log
@Component
@RequiredArgsConstructor
public class UserMentionEventHandler {

    private final UserService userService;
    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserMentionCreatedEvent event) {
        log.info("AFTER_COMMIT -> Mention created commentId={}, authorId={}, mentionedUserIds={}",
                event.commentId(), event.authorEmail(), event.mentionedUserIds());

        List<User> mentionedUsers = userService.findAllUsersByid(event.mentionedUserIds(), event.orgId());

        List<Notification> notis = mentionedUsers.stream()
                .map(targetUser -> {
                    Notification noti = new Notification();

                    noti.setTargetUser(targetUser);
                    noti.setMessage(event.authorEmail() + " has mentioned you");
                    noti.setDetails(event.briefComment());
                    noti.setCommentId(event.commentId());
                    noti.setType(NotificationType.MENTION);
                    noti.setRfcId(event.rfcId());

                    return noti;
                })
                .toList();

        notificationRepository.saveAll(notis);

    }
}
