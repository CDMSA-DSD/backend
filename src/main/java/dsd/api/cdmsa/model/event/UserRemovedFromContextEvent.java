package dsd.api.cdmsa.model.event;

public record UserRemovedFromContextEvent(
        Long orgId,
        Long contextId,
        String contextName,
        Long targetUserId,
        String actingAdminEmail
) {}
