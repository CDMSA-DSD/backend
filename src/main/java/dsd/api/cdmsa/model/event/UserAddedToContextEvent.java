package dsd.api.cdmsa.model.event;

public record UserAddedToContextEvent(
        Long orgId,
        Long contextId,
        String contextName,
        Long targetUserId,
        String actingAdminEmail
) {}
