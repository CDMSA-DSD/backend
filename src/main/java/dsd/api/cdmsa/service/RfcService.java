package dsd.api.cdmsa.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dsd.api.cdmsa.dto.AlternativeResponse;
import dsd.api.cdmsa.dto.CloseRfcRequest;
import dsd.api.cdmsa.dto.CommentResponse;
import dsd.api.cdmsa.dto.CreateAlternativeRequest;
import dsd.api.cdmsa.dto.CreateCommentRequest;
import dsd.api.cdmsa.dto.CreateRfcRequest;
import dsd.api.cdmsa.dto.RfcResponse;
import dsd.api.cdmsa.exception.RfcAlternativeBadRequestException;
import dsd.api.cdmsa.exception.RfcAlternativeNotAllowedException;
import dsd.api.cdmsa.exception.RfcBadRequestException;
import dsd.api.cdmsa.exception.RfcDependencyNotFoundException;
import dsd.api.cdmsa.exception.RfcInvalidStatusException;
import dsd.api.cdmsa.exception.RfcNotFoundException;
import dsd.api.cdmsa.model.Alternative;
import dsd.api.cdmsa.model.Comment;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.repository.AlternativeRepository;
import dsd.api.cdmsa.repository.CommentRepository;
import dsd.api.cdmsa.repository.OrganizationRepository;
import dsd.api.cdmsa.repository.RfcRepository;
import dsd.api.cdmsa.repository.TemplateRepository;
import dsd.api.cdmsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RfcService {
    // Existing repositories
    private final RfcRepository rfcRepository;
    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;
    private final AlternativeRepository alternativeRepository;
    private final CommentRepository commentRepository;

    private final AdrService adrService;

    @Transactional
    public RfcResponse postCommentToRfc(Long rfcId, Long userId, CreateCommentRequest request) {
        // in the future we should check that the user must be a reviewer of the RFC in
        // order to post a comment
        // ...

        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RuntimeException("RFC not found"));

        Comment comment = new Comment();
        comment.setAuthor(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
        comment.setContent(request.content().trim());
        comment.setRfc(rfc);

        rfc.getComments().add(comment);
        rfcRepository.save(rfc); // saved also in the table comments thanks to cascade all

    List<CommentResponse> commentResponses = rfc.getComments().stream()
        .map(c -> new CommentResponse(
            c.getId(),
            c.getAuthor() != null ? c.getAuthor().getId() : null,
            c.getAuthor() != null ? c.getAuthor().getEmail() : null,  // Before getUsername
            c.getContent(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        ))
        .toList();

    // Build full RfcResponse using the record constructor arguments order
    return new RfcResponse(
        rfc.getId(),
        rfc.getTitle(),
        rfc.getDescription(),
        rfc.getUser() != null ? rfc.getUser().getId() : null,
        rfc.getUser() != null ? rfc.getUser().getFirstname() : null, //Before getName
        rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
        rfc.getOrg() != null ? rfc.getOrg().getId() : null,
        rfc.getStatus(),
        rfc.getCreatedAt(),
        rfc.getUpdatedAt(),
        java.util.List.of(), // alternatives (lightweight here)
        commentResponses
    );
    }

    // ---------- US-12: Create RFC ----------

    @Transactional
    public RfcResponse createRfc(Long userId, Long orgId, CreateRfcRequest request) {

        // Validate request body
        if (request == null) {
            throw new RfcBadRequestException("Request body cannot be null");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new RfcBadRequestException("Title is required");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new RfcBadRequestException("Description is required");
        }
        if (request.templateId() == null) {
            throw new RfcBadRequestException("TemplateId is required");
        }

        // Validate related entities
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RfcDependencyNotFoundException("User (author) not found"));

        var template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new RfcDependencyNotFoundException("Template not found"));

        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RfcDependencyNotFoundException("Organization not found"));

        // Build RFC entity
        RFC rfc = new RFC();
        rfc.setTitle(request.title().trim());
        rfc.setDescription(request.description().trim());
        rfc.setTemplate(template);
        rfc.setOrg(org);
        rfc.setUser(user);
        // Status defaults to UNDER_REVIEW in RFC model

        // Persist and map
        RFC saved = rfcRepository.save(rfc);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<RfcResponse> listRfcs(Pageable pageable) {
        return rfcRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * Returns a paginated list of RFCs that belong to the specified organization.
     * This is the org-scoped variant used by controllers to ensure users only
     * see RFCs belonging to their organization.
     */
    @Transactional(readOnly = true)
    public Page<RfcResponse> listRfcsByOrg(Long orgId, Pageable pageable) {
        return rfcRepository.findByOrgId(orgId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RfcResponse getRfcById(Long rfcId) {
        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));
        return toDetailedResponse(rfc);
    }

    /**
     * Returns a RFC only if it belongs to the given organization. Used to
     * ensure organization-scoped access to RFC details.
     */
    @Transactional(readOnly = true)
    public RfcResponse getRfcByIdForOrg(Long rfcId, Long orgId) {
        RFC rfc = rfcRepository.findByIdAndOrgId(rfcId, orgId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));
        return toDetailedResponse(rfc);
    }

    private RfcResponse toResponse(RFC rfc) {
    return new RfcResponse(
        rfc.getId(),
        rfc.getTitle(),
        rfc.getDescription(),
        rfc.getUser() != null ? rfc.getUser().getId() : null,
        rfc.getUser() != null ? rfc.getUser().getFirstname() : null, //Before getName
        rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
        rfc.getOrg() != null ? rfc.getOrg().getId() : null,
        rfc.getStatus(),
        rfc.getCreatedAt(),
        rfc.getUpdatedAt(),
        java.util.List.of(), // lightweight list for summary
        java.util.List.of());
    }

    private RfcResponse toDetailedResponse(RFC rfc) {
        List<AlternativeResponse> alts = alternativeRepository.findByRfcId(rfc.getId()).stream()
                .map(AlternativeResponse::fromEntity)
                .collect(Collectors.toList());

    List<CommentResponse> comments = commentRepository.findByRfcId(rfc.getId()).stream()
        .map(c -> new CommentResponse(
            c.getId(),
            c.getAuthor() != null ? c.getAuthor().getId() : null,
            c.getAuthor() != null ? c.getAuthor().getEmail() : null, //Before getUsernam
            c.getContent(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        ))
        .collect(Collectors.toList());

    return new RfcResponse(
        rfc.getId(),
        rfc.getTitle(),
        rfc.getDescription(),
        rfc.getUser() != null ? rfc.getUser().getId() : null,
        rfc.getUser() != null ? rfc.getUser().getFirstname() : null, // Before getName
        rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
        rfc.getOrg() != null ? rfc.getOrg().getId() : null,
        rfc.getStatus(),
        rfc.getCreatedAt(),
        rfc.getUpdatedAt(),
        alts,
        comments);
    }

    // ---------- Alternatives (POST & GET) ----------

    @Transactional
    public AlternativeResponse addAlternative(Long rfcId, Long userId, CreateAlternativeRequest request) {

        // Validate basic payload
        if (request == null) {
            throw new RfcAlternativeBadRequestException("Request body cannot be null");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new RfcAlternativeBadRequestException("Title is required");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new RfcAlternativeBadRequestException("Description is required");
        }

        // Load RFC
        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));

        // Check status: only UNDER_REVIEW allows new alternatives
        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException(
                    "Alternatives can only be created for RFCs in UNDER_REVIEW status");
        }

        // Check authorization: only RFC author can create alternatives
        if (!rfc.getUser().getId().equals(userId)) {
            throw new RfcAlternativeNotAllowedException(
                    "Only the author of the RFC is allowed to create alternatives");
        }

        // Build and persist alternative
        Alternative alternative = new Alternative();
        alternative.setRfc(rfc);
        alternative.setTitle(request.title().trim());
        alternative.setDescription(request.description().trim());
        alternative.setAuthor(rfc.getUser());

        // If pros/cons exist in the request and entity:
        alternative.setPros(request.pros());
        alternative.setCons(request.cons());

        Alternative saved = alternativeRepository.save(alternative);
        // Update RFC updated timestamp to reflect new alternative
        rfc.setUpdatedAt(java.time.Instant.now());
        rfcRepository.save(rfc);

        return AlternativeResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AlternativeResponse> listAlternatives(Long rfcId) {

        // Ensure RFC exists (helps return 404 instead of empty list for invalid id)
        if (!rfcRepository.existsById(rfcId)) {
            throw new RfcNotFoundException("RFC not found with id " + rfcId);
        }

        return alternativeRepository.findByRfcId(rfcId).stream()
                .map(AlternativeResponse::fromEntity)
                .toList();
    }

    /**
     * List alternatives but only if the RFC belongs to the given organization.
     */
    @Transactional(readOnly = true)
    public List<AlternativeResponse> listAlternativesForOrg(Long rfcId, Long orgId) {
        if (!rfcRepository.existsByIdAndOrgId(rfcId, orgId)) {
            throw new RfcNotFoundException("RFC not found with id " + rfcId);
        }
        return alternativeRepository.findByRfcId(rfcId).stream()
                .map(AlternativeResponse::fromEntity)
                .toList();
    }

    @Transactional
    public RfcResponse closeRfc(Long rfcId, Long userId, CloseRfcRequest request) {
        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));

        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException("RFC is not under review");
        }

        if (rfc.getUser().getId() != userId) {
            throw new RfcAlternativeNotAllowedException("Only the author can close this RFC");
        }

        // Case 1: closed with alternative (US-22)
        if (request.alternativeId() != null) {
            Alternative winningAlt = alternativeRepository.findById(request.alternativeId())
                    .orElseThrow(
                            () -> new RfcNotFoundException("Alternative not found with id " + request.alternativeId()));

            if (!winningAlt.getRfc().getId().equals(rfc.getId())) {
                throw new RfcAlternativeBadRequestException("Alternative does not belong to this RFC");
            }

            rfc.setStatus(RFC.Status.CLOSED_DECIDED);
            // Link winning alternative
            // rfc.setWinningAlternative(winningAlt);

            // ADR adr = adrService.createDraftFromRfcAndAlternative(rfc, winningAlt);
            // rfc.setAdr(adr);
        }
        // Case 2: closed without alternative (US-23)
        else {
            rfc.setStatus(RFC.Status.CLOSED_NON_DECIDED);

        }

        RFC saved = rfcRepository.save(rfc);
        return toResponse(saved);
    }

}
