package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.SearchResult;
import dsd.api.cdmsa.dto.SearchResultProjection;
import dsd.api.cdmsa.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, String sortParam, Long authorId, Instant dateFrom, Instant dateTo) {

        // Avoid too short searches
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        // Format for Boolean Mode
        // Input: "login error" -> Output: "+login* +error*"
        // This allows that is the user search for Migr instead of Migration, he still gets the results
        StringBuilder sb = new StringBuilder();
        String[] words = query.trim().split("\\s+");

        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z0-9À-ÿ]", "");
            if (!clean.isEmpty()) {
                sb.append("+").append(clean).append("* ");
            }
        }

        String formattedQuery = sb.toString().trim();
        if (formattedQuery.isEmpty()) return List.of();

        // Execute query, the default sort is by relevance
        String sort = (sortParam == null || sortParam.isEmpty()) ? "relevance" : sortParam;

        List<SearchResultProjection> rawResults = searchRepository.searchSimple(
                formattedQuery,
                sort,
                authorId,
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