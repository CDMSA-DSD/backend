package dsd.api.cdmsa.dto;

import java.util.List;


public record ReviewersRequest(
    List<Long> userIds,
    List<Long> contextIds
) {}
