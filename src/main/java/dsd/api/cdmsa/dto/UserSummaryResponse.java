package dsd.api.cdmsa.dto;

import org.springframework.hateoas.server.core.Relation;

import dsd.api.cdmsa.model.User;

@Relation(collectionRelation = "users")
public record UserSummaryResponse(
        Long id,
        String email) {

    public static UserSummaryResponse fromEntity(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail());
    }
}