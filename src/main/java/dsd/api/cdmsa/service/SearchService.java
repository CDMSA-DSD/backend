package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.SearchResult;
import dsd.api.cdmsa.dto.SearchResultProjection;
import dsd.api.cdmsa.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, String sortParam, Long authorId, Long orgId, Instant dateFrom, Instant dateTo) {

        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        String[] words = query.trim().split("\\s+");
        List<String> pgWords = new ArrayList<>();

        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z0-9À-ÿ]", "");
            if (!clean.isEmpty()) {
                pgWords.add(clean + ":*");
            }
        }

        if (pgWords.isEmpty()) return List.of();

        String formattedQuery = String.join(" & ", pgWords);

        // Default sort
        String sort = (sortParam == null || sortParam.isEmpty()) ? "relevance" : sortParam;

        List<SearchResultProjection> rawResults = searchRepository.searchSimple(
                formattedQuery,
                sort,
                authorId,
                orgId,
                dateFrom,
                dateTo
        );

        return rawResults.stream()
                .map(r -> new SearchResult(
                        r.getId(),
                        r.getTitle(),
                        r.getSnippet(),
                        r.getAuthorName(),
                        r.getDate(),
                        r.getType(),
                        r.getScore()
                ))
                .toList();
    }
}