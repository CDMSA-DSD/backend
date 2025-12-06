package dsd.api.cdmsa.dto;

import java.util.List;

import org.springframework.hateoas.EntityModel;

import dsd.api.cdmsa.model.RFC;

public record RfcSpecificResponse(
        Long id,
        String title,
        String description,
        Long userId,
        String authorFirstname,
        String authorLastname,
        Long templateId,
        Long orgId,
        RFC.Status status,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        boolean isAuthor,
        List<EntityModel<UserSummaryResponse>> userReviewers,
        List<EntityModel<ContextSummaryResponse>> contextReviewers,
        List<AlternativeResponse> alternatives,
        List<CommentResponse> comments) {

}
