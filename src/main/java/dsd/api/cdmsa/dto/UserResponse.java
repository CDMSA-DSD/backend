package dsd.api.cdmsa.dto;

import java.time.Instant;

import org.springframework.hateoas.server.core.Relation;

import dsd.api.cdmsa.model.User;

@Relation(collectionRelation = "users")
public record UserResponse(
        Long id,
        String firstname,
        String lastName,
        String email,
        Instant joinedAt) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getJoinedAt());
    }
}