package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.SearchResult;
import dsd.api.cdmsa.service.SearchService;
import dsd.api.cdmsa.exception.OrgNotFoundException;
import dsd.api.cdmsa.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/search")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    private Long getCurrentUserOrgId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            Long orgId = userPrincipal.getOrgId();
            if (orgId != null) return orgId;
        }
        throw new OrgNotFoundException(0L);
    }

    @GetMapping
    public List<SearchResult> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        Long orgId = getCurrentUserOrgId();

        Instant from = null;
        Instant to = null;
        try {
            if (dateFrom != null) from = Instant.parse(dateFrom + "T00:00:00Z");
            if (dateTo != null)   to   = Instant.parse(dateTo + "T23:59:59Z");
        } catch (Exception e) {
        }

        return service.search(q, sort, authorId, orgId, from, to);
    }
}