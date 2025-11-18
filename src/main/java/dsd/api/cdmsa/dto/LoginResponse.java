package dsd.api.cdmsa.dto;

public record LoginResponse(
        Long id,
        String firstname,
        String lastName,
        String email
        ) {

}
