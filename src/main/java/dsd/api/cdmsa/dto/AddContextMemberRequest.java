package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record AddContextMemberRequest(@NotBlank String email) {
}
