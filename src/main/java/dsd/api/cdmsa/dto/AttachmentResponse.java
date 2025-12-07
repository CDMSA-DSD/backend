package dsd.api.cdmsa.dto;

public record AttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long size,
        String downloadUrl
) {}
