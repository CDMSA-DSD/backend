package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.service.LLMService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dsd.api.cdmsa.service.RfcService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import dsd.api.cdmsa.model.User;
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
    private final LLMService llmService;

    // Post a new comment under the rfc identified by id
    @PostMapping("/{id}/comments")
    @PreAuthorize("@permissionService.canReviewRfc(principal,#id)")
    public ResponseEntity<RfcResponse> postCommentToRfc(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        RfcResponse response = rfcService.postCommentToRfc(id, user, request);

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
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        RfcResponse response = rfcService.createRfc(user.getId(), principal.getOrgId(), request);

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
            @AuthenticationPrincipal UserPrincipal principal) {

        return rfcService.listRfcsByOrg(principal.getOrgId(), pageable);
    }

    /**
     * Returns a specific RFC by its ID.
     */
    @GetMapping("/{rfcId}")
    public ResponseEntity<RfcSpecificResponse> getRfcById(
            @PathVariable Long rfcId,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        RfcSpecificResponse response = rfcService.getRfcByIdForOrg(rfcId, principal.getOrgId(), user.getId());

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
    @PreAuthorize("@permissionService.canReviewRfc(principal,#id)")
    public ResponseEntity<AlternativeResponse> createAlternative(
            @PathVariable Long rfcId,
            @RequestBody CreateAlternativeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        AlternativeResponse response = rfcService.addAlternative(rfcId, user.getId(), request);

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
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AlternativeResponse> alternatives = rfcService.listAlternativesForOrg(rfcId, principal.getOrgId());

        return ResponseEntity.ok(alternatives);
    }

    // ------------------------ Close RFC ----------------------

    @PostMapping("/{rfcId}/close")
    @PreAuthorize("@permissionService.canManageRfc(principal,#id)")
    public ResponseEntity<RfcResponse> closeRfc(
            @PathVariable Long rfcId,
            @RequestBody CloseRfcRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        RfcResponse response = rfcService.closeRfc(rfcId, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/alternatives/{altId}/vote")
    @PreAuthorize("@permissionService.canReviewRfc(principal,#id)")
    public ResponseEntity<VoteResponse> voteForAlternative(
            @PathVariable Long altId,
            @RequestBody VoteRequest voteRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        // check if user is a reviewer, probably we need also the rfcId to see status =
        // under review
        User user = principal.getUser();

        VoteResponse resp = rfcService.voteForAlternative(altId, user.getId(), voteRequest.outcome());

        return ResponseEntity.ok(resp);
    }

    // generate ADR of the RFC using LLM
    @PostMapping("/{rfcId}/generateadr")
    @PreAuthorize("@permissionService.canManageRfc(principal,#id)")
    public ResponseEntity<GenerateAdrResponse> createDraftFromRfcAndAlternative(
            @PathVariable Long rfcId,
            @RequestBody GenerateAdrRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        GenerateAdrResponse response = llmService.createDraftFromRfcAndAlternative(rfcId, user.getId(), request);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/reviewers")
    @PreAuthorize("@permissionService.canManageRfc(principal,#id)")
    public ResponseEntity<Void> asignReviewersToRfc(
            @PathVariable Long id,
            @RequestBody ReviewersRequest reviewers) {

        rfcService.asignReviewersToRfc(reviewers, id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "{rfcId}/subscribe")
    public ResponseEntity<Void> subscribeToRfc(
            @PathVariable Long rfcId,
            @AuthenticationPrincipal UserPrincipal principal) {

        rfcService.subscribeToRfc(rfcId, principal);

        return ResponseEntity.noContent().build();
    }

}
