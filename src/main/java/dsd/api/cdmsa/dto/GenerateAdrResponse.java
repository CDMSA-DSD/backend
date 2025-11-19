package dsd.api.cdmsa.dto;

public record GenerateAdrResponse(
        String title,
        String context,
        String decision,
        String consequences) {
}
