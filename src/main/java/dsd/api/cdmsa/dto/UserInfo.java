package dsd.api.cdmsa.dto;

public record UserInfo(
        String email,
        String firstname,
        String lastname,
        String provider,
        String providerId) {
}
