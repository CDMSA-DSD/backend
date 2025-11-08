package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.AdrResponse;
import dsd.api.cdmsa.dto.CreateAdrRequest;
import dsd.api.cdmsa.service.AdrService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/adrs")
@RequiredArgsConstructor
public class AdrController {

    private final AdrService adrService;

    /**
     * Retrieves the current user id from the request.
     * For this sprint we use a simple header-based approach ("X-User-Id"),
     * which should be replaced by a proper authentication mechanism later.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            // For development/demo purposes only.
            // In production, this must be replaced by authenticated user context.
            return 1L;
        }
        return Long.parseLong(header);
    }


    // Create an ADR associated with an RFC (RFC id is sent from the frontend) -- wrote this method with the idea that ADR was filled by the user, see the last method to build it from RFC
    @PostMapping
    public ResponseEntity<AdrResponse> createAdr(@Valid @RequestBody CreateAdrRequest request, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged in ...
        Long userId = getCurrentUserId(httpRequest);

        AdrResponse response = adrService.createAdr(request);
        // build the markdown file from form, push it on GitHub through API, store it in the db (the url)
        // ...
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // Get a specific ADR from its id
    @GetMapping("/{id}")
    public ResponseEntity<AdrResponse> getAdrById(@PathVariable Long id, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged in ...
        Long userId = getCurrentUserId(httpRequest);

        AdrResponse response = adrService.getAdrById(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public Page<AdrResponse> listAdrs(
            @PageableDefault(size = 20) Pageable pageable) {
        return adrService.listAdrs(pageable);
    }

    /* it is necessary?

    public ADR createDraftFromRfcAndAlternative(RFC rfc, Alternative alternative) {
        ADR adr = new ADR();
        adr.setRfc(rfc);
        adr.setStatus(ADR.Status.DRAFT);
        adr.setTitle("ADR for RFC #" + rfc.getId() + ": " + rfc.getTitle());
        adr.setContext(rfc.getDescription());
        adr.setDecision("Selected alternative: " + alternative.getTitle());
        return adrRepository.save(adr);
    }

     */


}

