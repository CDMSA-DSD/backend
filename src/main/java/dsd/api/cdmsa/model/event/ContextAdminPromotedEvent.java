package dsd.api.cdmsa.model.event;

public record ContextAdminPromotedEvent(
        Long orgId,
        Long contextId,
        String contextName,
        Long targetUserId,
        String actingAdminEmail
) {}