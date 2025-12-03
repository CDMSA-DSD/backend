package dsd.api.cdmsa.model.event;

import java.util.List;

public record UserMentionCreatedEvent(
    Long commentId,
    Long authorId,
    List<Long> mentionedUserIds
) {}
