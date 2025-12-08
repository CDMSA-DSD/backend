package dsd.api.cdmsa.model.event;

import java.util.List;

public record UserMentionCreatedEvent(
    Long commentId,
    Long orgId,
    Long rfcId,
    String briefComment,
    String authorEmail,
    List<Long> mentionedUserIds
) {}
