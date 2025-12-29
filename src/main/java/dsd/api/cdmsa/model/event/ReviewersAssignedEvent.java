package dsd.api.cdmsa.model.event;

import java.util.List;

public record ReviewersAssignedEvent(
        Long orgId,
        Long rfcId,
        String rfcTitle,
        List<Long> userIds,
        List<Long> contextIds,
        String authorEmail
) {}
