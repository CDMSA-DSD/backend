package dsd.api.cdmsa.controller;

import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.exception.OrgNotFoundException;
import dsd.api.cdmsa.exception.UserNotFoundException;
import dsd.api.cdmsa.model.AlternativeAttachment;
import dsd.api.cdmsa.model.RfcAttachment;
import dsd.api.cdmsa.service.LLMService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dsd.api.cdmsa.service.RfcService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

import dsd.api.cdmsa.model.User;
import dsd.api.cdmsa.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

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

    // Post a new comment under the rfc identified by id
    @PostMapping("/{id}/comments")
    @PreAuthorize("@permissionService.canReviewRfc(principal,#id)")
    public ResponseEntity<RfcResponse> postCommentToRfc(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        RfcResponse response = rfcService.postCommentToRfc(id, user, principal.getOrgId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a new RFC.
     *
     * HTTP 201 Created on success.
     * Validation and business rules are handled inside RfcService.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RfcResponse> createRfc(
            // JSON part
            @RequestPart("data") @Valid CreateRfcRequest request,
            // Attachments part (optional)
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        User user = principal.getUser();
        RfcResponse response = rfcService.createRfc(user.getId(), principal.getOrgId(), request, files);

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
    @PostMapping(value = "/{rfcId}/alternatives", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionService.canReviewRfc(principal,#rfcId)")
    public ResponseEntity<AlternativeSpecificResponse> createAlternative(
            @PathVariable Long rfcId,
            @RequestPart("data") @Valid CreateAlternativeRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {


        User user = principal.getUser();
        AlternativeSpecificResponse response = rfcService.addAlternative(rfcId, user.getId(), request, files);

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
    @PreAuthorize("@permissionService.canManageRfc(principal,#rfcId)")
    public ResponseEntity<RfcResponse> closeRfc(
            @PathVariable Long rfcId,
            @RequestBody CloseRfcRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        RfcResponse response = rfcService.closeRfc(rfcId, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/alternatives/{altId}/vote")
    @PreAuthorize("@permissionService.canReviewRfc(principal,#altId)")
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
    @PreAuthorize("@permissionService.canManageRfc(principal,#rfcId)")
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
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody ReviewersRequest reviewers) {

        rfcService.asignReviewersToRfc(reviewers, id, principal.getOrgId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "{rfcId}/subscribe")
    public ResponseEntity<Void> subscribeToRfc(
            @PathVariable Long rfcId,
            @AuthenticationPrincipal UserPrincipal principal) {

        rfcService.subscribeToRfc(rfcId, principal);

        return ResponseEntity.noContent().build();
    }

    // UPDATE OR CREATE DIAGRAM
    @PutMapping("/{rfcId}/diagram")
    public ResponseEntity<RfcSpecificResponse> updateOrCreateRfcDiagram(
            @PathVariable Long rfcId,
            @RequestBody UpdateCreateDiagramRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        RfcSpecificResponse updatedRfc = rfcService.updateOrCreateDiagram(rfcId, userId, request.xmlContent());

        return ResponseEntity.ok(updatedRfc);
    }

    // UPLOAD ATTACHMENTS
    @PostMapping(value = "/{rfcId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<AttachmentResponse>> uploadAttachmentsToRfc(
            @PathVariable Long rfcId,
            @RequestParam("files") List<MultipartFile> files, // List of attachments
            HttpServletRequest httpRequest) throws IOException {

        Long userId = getCurrentUserId(httpRequest);

        List<RfcAttachment> attachments = rfcService.uploadMultipleAttachments(rfcId, userId, files);

        List<AttachmentResponse> response = attachments.stream()
                .map(att -> new AttachmentResponse(
                        att.getId(),
                        att.getFileName(),
                        att.getContentType(),
                        att.getSize(),
                        "/rfcs/attachments/download/RFC/" + att.getId()
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // probably not needed
    @GetMapping("/{rfcId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsForRfc(@PathVariable Long rfcId) {

        List<RfcAttachment> entities = rfcService.getRfcAttachments(rfcId);

        List<AttachmentResponse> dtos = entities.stream()
                .map(att -> new AttachmentResponse(
                        att.getId(),
                        att.getFileName(),
                        att.getContentType(),
                        att.getSize(),
                        "/rfcs/attachments/download/RFC/" + att.getId()))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // DOWNLOAD
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId, HttpServletRequest httpRequest) {

        Long userOrgId = getCurrentUserOrgId(httpRequest);

        RfcService.FileDownloadDTO fileData = rfcService.downloadRfcAttachment(attachmentId, userOrgId);

        return ResponseEntity.ok()
                // Set type of file (image/png for example)
                .contentType(MediaType.parseMediaType(fileData.contentType()))

                // Header
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileData.fileName() + "\"")

                .body(fileData.resource());
    }

    @PutMapping("/{rfcId}")
    public ResponseEntity<RfcSpecificResponse> updateRfcFields(
            @PathVariable Long rfcId,
            @Valid @RequestBody UpdateRfcRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        Long orgId = getCurrentUserOrgId(httpRequest);

        RfcSpecificResponse response = rfcService.updateRfcText(rfcId, userId, orgId, request);

        return ResponseEntity.ok(response);
    }


    // Get specific alternative
    @GetMapping("/alternatives/{altId}")
    public ResponseEntity<AlternativeSpecificResponse> getAlternativeById(
            @PathVariable Long altId,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        Long orgId = getCurrentUserOrgId(httpRequest);

        AlternativeSpecificResponse response = rfcService.getAlternativeByIdForOrg(altId, orgId, userId);
        return ResponseEntity.ok(response);
    }

    // Alternative diagram
    @PutMapping("/alternatives/{altId}/diagram")
    public ResponseEntity<AlternativeSpecificResponse> updateOrCreateAlternativeDiagram(
            @PathVariable Long altId,
            @RequestBody UpdateCreateDiagramRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        AlternativeSpecificResponse response = rfcService.updateOrCreateAlternativeDiagram(altId, userId, request.xmlContent());
        return ResponseEntity.ok(response);
    }

    // Alternative attachments
    @PostMapping(value = "/alternatives/{altId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<AttachmentResponse>> uploadAttachmentsToAlternative(
            @PathVariable Long altId,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest httpRequest) throws IOException {

        Long userId = getCurrentUserId(httpRequest);

        List<AlternativeAttachment> attachments = rfcService.uploadMultipleAlternativeAttachments(altId, userId, files);

        List<AttachmentResponse> response = attachments.stream()
                .map(att -> new AttachmentResponse(
                        att.getId(),
                        att.getFileName(),
                        att.getContentType(),
                        att.getSize(),
                        "/rfcs/alternatives/attachments/download/" + att.getId()
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Download attachment
    @GetMapping("/alternatives/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAlternativeAttachment(
            @PathVariable Long attachmentId,
            HttpServletRequest httpRequest) {

        Long userOrgId = getCurrentUserOrgId(httpRequest);

        RfcService.FileDownloadDTO fileData = rfcService.downloadAlternativeAttachment(attachmentId, userOrgId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileData.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileData.fileName() + "\"")
                .body(fileData.resource());
    }

    @PutMapping("/alternatives/{altId}")
    public ResponseEntity<AlternativeSpecificResponse> updateAlternativeFields(
            @PathVariable Long altId,
            @RequestBody UpdateAlternativeRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);

        AlternativeSpecificResponse response = rfcService.updateAlternative(altId, userId, request);

        return ResponseEntity.ok(response);
    }
}
