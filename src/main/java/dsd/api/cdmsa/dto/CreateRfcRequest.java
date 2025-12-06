package dsd.api.cdmsa.dto;

import java.util.List;

public record CreateRfcRequest(
        String title,
        String description,
        Long templateId,
        List<Long> userReviewerIds,
        List<Long> contextReviewerIds
) {
}
