package dsd.api.cdmsa.dto;

public record PartecipatingUser(
        Long userId,
        String fullName,
        String role
) {}