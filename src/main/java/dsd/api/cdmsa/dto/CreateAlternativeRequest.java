package dsd.api.cdmsa.dto;

public record CreateAlternativeRequest(
                String title,
                String description,
                String pros,
                String cons,
                String xml) {
}
