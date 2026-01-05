package dsd.api.cdmsa.model.event;

public record ContextAdminDemotedEvent(
        Long orgId,
        Long contextId,
        String contextName,
        Long targetUserId,
        String actingAdminEmail
) {}