package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record MSOrgSignInRequest( 
    @NotBlank String code,
    OrganizationResponse org) {}

