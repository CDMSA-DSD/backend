package dsd.api.cdmsa.dto;

import java.time.Instant;

import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "users")
public record UserResponse(
    Long id,
    String firstname,
    String lastName,
    String email,
    Instant joinedAt,
    String jobTitle
    ) {}