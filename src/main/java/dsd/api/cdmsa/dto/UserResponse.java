package dsd.api.cdmsa.dto;

import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "users")
public record UserResponse(
    Long id,
    String firstname,
    String lastName,
    String email
    ) {}