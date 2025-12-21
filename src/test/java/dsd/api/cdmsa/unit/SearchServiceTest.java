package dsd.api.cdmsa.unit;

import dsd.api.cdmsa.dto.SearchResult;
import dsd.api.cdmsa.dto.SearchResultProjection;
import dsd.api.cdmsa.repository.SearchRepository;
import dsd.api.cdmsa.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Tests")
class SearchServiceTest {

    @Mock
    private SearchRepository searchRepository;

    @InjectMocks
    private SearchService searchService;

    // ==================== HAPPY PATH & QUERY FORMATTING ====================

    @Test
    @DisplayName("Should format query correctly and map results")
    void shouldFormatQueryAndMapResults() {
        // Given
        String inputQuery = "microservices java";
        String expectedFormattedQuery = "microservices:* & java:*";

        // Mocking the projection (assuming it's an interface)
        SearchResultProjection mockProjection = mock(SearchResultProjection.class);
        when(mockProjection.getId()).thenReturn(10L);
        when(mockProjection.getTitle()).thenReturn("Intro to Microservices");
        when(mockProjection.getSnippet()).thenReturn("Microservices are...");
        when(mockProjection.getAuthorName()).thenReturn("Mario Rossi");
        when(mockProjection.getDate()).thenReturn(Instant.now());
        when(mockProjection.getType()).thenReturn("RFC");
        when(mockProjection.getScore()).thenReturn(0.95);

        when(searchRepository.searchSimple(
                eq(expectedFormattedQuery),
                eq("relevance"), // default check
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(mockProjection));

        // When
        List<SearchResult> results = searchService.search(inputQuery, null, null, null, null, null);

        // Then
        assertThat(results).hasSize(1);
        SearchResult res = results.get(0);
        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.title()).isEqualTo("Intro to Microservices");
        assertThat(res.type()).isEqualTo("RFC");

        verify(searchRepository).searchSimple(eq(expectedFormattedQuery), anyString(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should sanitize query by removing special characters")
    void shouldSanitizeQueryInput() {
        // Given
        String inputQuery = "hello, world! 123";
        // Logic removes [^a-zA-Z0-9À-ÿ], so "," and "!" go away.
        String expectedFormattedQuery = "hello:* & world:* & 123:*";

        when(searchRepository.searchSimple(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of());

        // When
        searchService.search(inputQuery, null, null, null, null, null);

        // Then
        verify(searchRepository).searchSimple(
                eq(expectedFormattedQuery),
                eq("relevance"),
                isNull(), isNull(), isNull(), isNull()
        );
    }

    // ==================== FILTERS & SORTING ====================

    @Test
    @DisplayName("Should pass filters and custom sort to repository")
    void shouldPassFiltersAndSortToRepository() {
        // Given
        String query = "test";
        String sort = "date_desc";
        Long authorId = 55L;
        Long orgId = 99L;
        Instant from = Instant.parse("2023-01-01T00:00:00Z");
        Instant to = Instant.parse("2023-12-31T23:59:59Z");

        when(searchRepository.searchSimple(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of());

        // When
        searchService.search(query, sort, authorId, orgId, from, to);

        // Then
        verify(searchRepository).searchSimple(
                eq("test:*"), // "test" formatted
                eq(sort),
                eq(authorId),
                eq(orgId),
                eq(from),
                eq(to)
        );
    }

    // ==================== EDGE CASES & VALIDATION ====================

    @Test
    @DisplayName("Should return empty list and not call repo if query is null")
    void shouldReturnEmptyIfQueryIsNull() {
        List<SearchResult> results = searchService.search(null, null, null, null, null, null);

        assertThat(results).isEmpty();
        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("Should return empty list and not call repo if query is too short")
    void shouldReturnEmptyIfQueryIsTooShort() {
        // Length < 2
        List<SearchResult> results = searchService.search("a", null, null, null, null, null);

        assertThat(results).isEmpty();
        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("Should return empty list if query contains only special characters")
    void shouldReturnEmptyIfQueryIsOnlySpecialChars() {
        // Logic: cleaning replaces [^a-zA-Z0-9À-ÿ] with empty string.
        // If result is empty, pgWords is empty -> returns List.of()
        String query = "!!! @@@ ###";

        List<SearchResult> results = searchService.search(query, null, null, null, null, null);

        assertThat(results).isEmpty();
        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("Should handle whitespace only query")
    void shouldHandleWhitespaceOnlyQuery() {
        List<SearchResult> results = searchService.search("   ", null, null, null, null, null);

        assertThat(results).isEmpty();
        verifyNoInteractions(searchRepository);
    }
}