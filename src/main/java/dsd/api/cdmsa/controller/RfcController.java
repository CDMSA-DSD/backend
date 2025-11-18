package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.CreateCommentRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dsd.api.cdmsa.dto.AlternativeResponse;
import dsd.api.cdmsa.dto.CloseRfcRequest;
import dsd.api.cdmsa.dto.CreateAlternativeRequest;
import dsd.api.cdmsa.dto.CreateRfcRequest;
import dsd.api.cdmsa.dto.RfcResponse;
import dsd.api.cdmsa.service.RfcService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import dsd.api.cdmsa.exception.*;

import dsd.api.cdmsa.model.UserPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * REST controller responsible for handling RFC-related endpoints.
 * Exposes operations for creating and listing RFCs (US-12).
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/rfcs")
@RequiredArgsConstructor
public class RfcController {

    private final RfcService rfcService;

     private UserPrincipal getAuthenticatedPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        throw new UserNotFoundException("anonymous");
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            return getAuthenticatedPrincipal().getUser().getId();
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new UserNotFoundException("anonymous");
        }
    }

    /**
     * Returns the organization id for the current authenticated user.
     */
    private Long getCurrentUserOrgId(HttpServletRequest request) {
        try {
            Long orgId = getAuthenticatedPrincipal().getOrgId();
            if (orgId != null) return orgId;
            throw new OrgNotFoundException(0L);
        } catch (OrgNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new OrgNotFoundException(0L);
        }
    }

    // post a new comment under the rfc identified by id
    @PostMapping("/{id}/comments")
    public ResponseEntity<RfcResponse> postCommentToRfc(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request, HttpServletRequest httpRequest) {
        // TO DO - check if user is logged in ...

        Long userId = getCurrentUserId(httpRequest);
        RfcResponse response = rfcService.postCommentToRfc(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        Long orgId = getCurrentUserOrgId(httpRequest);
        RfcResponse response = rfcService.createRfc(userId, orgId, request);
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
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest httpRequest) {
        Long orgId = getCurrentUserOrgId(httpRequest);
        return rfcService.listRfcsByOrg(orgId, pageable);
    }

    /**
     * Returns a specific RFC by its ID.
     */
    @GetMapping("/{rfcId}")
    public ResponseEntity<RfcResponse> getRfcById(
            @PathVariable Long rfcId,
            HttpServletRequest httpRequest) {
        Long orgId = getCurrentUserOrgId(httpRequest);
        RfcResponse response = rfcService.getRfcByIdForOrg(rfcId, orgId);
        return ResponseEntity.ok(response);
    }

    // ---------- Create Alternative ----------

    /**
     * Creates a new alternative for a given RFC.
     *
     * Rules enforced in service:
     * - RFC must exist.
     * - RFC must be UNDER_REVIEW.
     * - Only the author of the RFC can create alternatives.
     *
     * Request body: CreateAlternativeRequest
     * Returns: AlternativeResponse
     * Status: 201 Created
     */
    @PostMapping("/{rfcId}/alternatives")
    public ResponseEntity<AlternativeResponse> createAlternative(
            @PathVariable Long rfcId,
            @RequestBody CreateAlternativeRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        AlternativeResponse response = rfcService.addAlternative(rfcId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---------- List Alternatives ----------

    /**
     * Returns all alternatives for a given RFC.
     *
     * Example:
     * GET /rfcs/{rfcId}/alternatives
     */
    @GetMapping("/{rfcId}/alternatives")
    public ResponseEntity<List<AlternativeResponse>> listAlternatives(
            @PathVariable Long rfcId,
            HttpServletRequest httpRequest) {
        // ensure alternatives are only returned for RFCs in the same organization
        Long orgId = getCurrentUserOrgId(httpRequest);
        List<AlternativeResponse> alternatives = rfcService.listAlternativesForOrg(rfcId, orgId);
        return ResponseEntity.ok(alternatives);
    }

    // ------------------------ Close RFC ----------------------

    @PostMapping("/{rfcId}/close")
    public ResponseEntity<RfcResponse> closeRfc(
            @PathVariable Long rfcId,
            @RequestBody CloseRfcRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        RfcResponse response = rfcService.closeRfc(rfcId, userId, request);
        return ResponseEntity.ok(response);
    }
}
