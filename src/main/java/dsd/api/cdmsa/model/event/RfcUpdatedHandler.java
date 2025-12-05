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
public class RfcUpdatedHandler {

    private final UserService userService;
    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RfcUpdatedEvent event) {
        List<User> subscribers = userService.findAllUsersByid(event.subscribersId(), event.orgId());

        List<Notification> notis = subscribers.stream()
                .map(targetUser -> {
                    Notification noti = new Notification();

                    noti.setTargetUser(targetUser);
                    noti.setMessage(event.rfcTitle() + " has been updated.");
                    noti.setType(NotificationType.RFC_UPDATE);
                    noti.setRfcId(event.rfcId());

                    return noti;
                })
                .toList();

        notificationRepository.saveAll(notis);

    }
}
