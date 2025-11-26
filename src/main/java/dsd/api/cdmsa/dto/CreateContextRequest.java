package dsd.api.cdmsa.dto;

public record CreateContextRequest(
        String name, // required
        String type, // optional
        String description) { // optional
}
