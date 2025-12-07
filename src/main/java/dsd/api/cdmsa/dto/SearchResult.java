package dsd.api.cdmsa.dto;

import java.time.Instant;

/**
 * DTO immutabile da inviare al client.
 */
public record SearchResult(
        Long id,
        String title,
        String snippet,
        String authorName,
        Instant date,
        String type,
        Double score
) {}