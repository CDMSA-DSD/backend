package dsd.api.cdmsa.dto;

import java.time.Instant;

/**
 * Interfaccia utilizzata da Spring Data JPA per mappare i risultati
 * delle Native Query (SQL puro).
 */
public interface SearchResultProjection {
    Long getId();
    String getTitle();
    String getSnippet();
    String getAuthorName();
    Instant getDate();
    String getType();
    Double getScore();
}