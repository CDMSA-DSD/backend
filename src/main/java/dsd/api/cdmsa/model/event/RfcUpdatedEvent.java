package dsd.api.cdmsa.model.event;

import java.util.List;

public record RfcUpdatedEvent(
    Long orgId,
    Long rfcId,
    String rfcTitle,
    List<Long> subscribersId
) {}
