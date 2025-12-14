package dsd.api.cdmsa.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatBotRequest(

        @NotBlank(message = "User input cannot be empty")
        String message

) {}