package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record MSSignInRequest(
    @NotBlank String code,
    String token) {}

