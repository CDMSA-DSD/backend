package dsd.api.cdmsa.model.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserMentionEventHandler {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserMentionCreatedEvent event) {
        log.info("AFTER_COMMIT -> Mention created commentId={}, authorId={}, mentionedUserIds={}",
                event.commentId(), event.authorId(), event.mentionedUserIds());
    }
}
