package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotNull;

public record PublishAdrRequest(
        @NotNull Long adrId) {
}
