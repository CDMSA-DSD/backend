package dsd.api.cdmsa.dto;


import dsd.api.cdmsa.model.RFC;

import java.util.List;

public record RfcResponse(
        Long id,
        String title,
        String description,
        Long userId,
        Long templateId,
        Long orgId,
        RFC.Status status,
        List<CommentResponse> comments) {
}
