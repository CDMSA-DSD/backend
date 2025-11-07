package dsd.api.cdmsa.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dsd.api.cdmsa.dto.CreateRfcRequest;
import dsd.api.cdmsa.dto.RfcResponse;
import dsd.api.cdmsa.service.RfcService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * REST controller responsible for handling RFC-related endpoints.
 * Exposes operations for creating and listing RFCs (US-12).
 */
@RestController
@RequestMapping("/rfcs")
@RequiredArgsConstructor
public class RfcController {

    private final RfcService rfcService;

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

    /**
     * Creates a new RFC.
     *
     * HTTP 201 Created on success.
     * Validation and business rules are handled inside RfcService.
     */
    @PostMapping
    public ResponseEntity<RfcResponse> createRfc(
            @RequestBody CreateRfcRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        RfcResponse response = rfcService.createRfc(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns a paginated list of RFCs.
     *
     * Example:
     * GET /rfcs?page=0&size=20
     */
    @GetMapping
    public Page<RfcResponse> listRfcs(
            @PageableDefault(size = 20) Pageable pageable) {
        return rfcService.listRfcs(pageable);
    }
}
