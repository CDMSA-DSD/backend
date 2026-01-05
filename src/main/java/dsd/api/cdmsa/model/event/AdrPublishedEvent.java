package dsd.api.cdmsa.model.event;

import java.util.List;

public record AdrPublishedEvent(
        Long orgId,
        Long rfcId,
        Long adrId,
        String adrTitle,
        List<Long> targetUserIds
) {}
