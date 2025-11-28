package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.SearchResult;
import dsd.api.cdmsa.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/search") // Endpoint base
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    // GET /search?q=relational&sort=relevance
    @GetMapping
    public List<SearchResult> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "relevance") String sort, // values: relevance, date_asc, date_desc
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String dateFrom, // for example 2024-01-01
            @RequestParam(required = false) String dateTo
    ) {
        Instant from = null;
        Instant to = null;
        try {
            if (dateFrom != null) from = Instant.parse(dateFrom + "T00:00:00Z");
            if (dateTo != null)   to   = Instant.parse(dateTo + "T23:59:59Z");
        } catch (Exception e) {
        }

        return service.search(q, sort, authorId, from, to);
    }
}