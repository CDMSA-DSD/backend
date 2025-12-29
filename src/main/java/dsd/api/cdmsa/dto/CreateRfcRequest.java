package dsd.api.cdmsa.dto;

import java.util.List;

public record CreateRfcRequest(
        String title,
        String description,
        Long templateId,
        String xml,  // if null no diagram has been created (yet)
        List<Long> userReviewerIds,
        List<Long> contextReviewerIds
        ) {
}
