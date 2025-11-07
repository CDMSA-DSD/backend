package dsd.api.cdmsa.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dsd.api.cdmsa.dto.AlternativeResponse;
import dsd.api.cdmsa.dto.CloseRfcRequest;
import dsd.api.cdmsa.dto.CreateAlternativeRequest;
import dsd.api.cdmsa.dto.CreateRfcRequest;
import dsd.api.cdmsa.dto.RfcResponse;
import dsd.api.cdmsa.exception.RfcAlternativeBadRequestException;
import dsd.api.cdmsa.exception.RfcAlternativeNotAllowedException;
import dsd.api.cdmsa.exception.RfcBadRequestException;
import dsd.api.cdmsa.exception.RfcDependencyNotFoundException;
import dsd.api.cdmsa.exception.RfcInvalidStatusException;
import dsd.api.cdmsa.exception.RfcNotFoundException;
import dsd.api.cdmsa.model.ADR;
import dsd.api.cdmsa.model.Alternative;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.repository.AlternativeRepository;
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

    private final AdrService adrService;

    // ---------- US-12: Create RFC ----------

    @Transactional
    public RfcResponse createRfc(Long userId, CreateRfcRequest request) {

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
        if (request.orgId() == null) {
            throw new RfcBadRequestException("OrgId is required");
        }

        // Validate related entities
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RfcDependencyNotFoundException("User (author) not found"));

        var template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new RfcDependencyNotFoundException("Template not found"));

        var org = organizationRepository.findById(request.orgId())
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

    private RfcResponse toResponse(RFC rfc) {
        return new RfcResponse(
                rfc.getId(),
                rfc.getTitle(),
                rfc.getDescription(),
                rfc.getUser().getId(),
                rfc.getTemplate().getId(),
                rfc.getOrg().getId(),
                rfc.getStatus());
    }

    // ---------- Alternatives (POST & GET) ----------

    /**
     * Creates a new alternative for a given RFC.
     *
     * Rules:
     * - RFC must exist.
     * - RFC must be in UNDER_REVIEW status.
     * - Only the RFC author is allowed to create alternatives.
     * - title and description are required.
     *
     * @param rfcId   ID of the target RFC
     * @param userId  ID of the current user (author candidate)
     * @param request Alternative creation payload
     * @return AlternativeResponse DTO
     */
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
        return AlternativeResponse.fromEntity(saved);
    }

    /**
     * Returns all alternatives for a given RFC.
     *
     * @param rfcId ID of the RFC
     * @return List of AlternativeResponse
     */
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
            // rfc.setWinningAlternative(winningAlt); // si tienes este campo

            // Create ADR draft (using AdrService)
            ADR adr = adrService.createDraftFromRfcAndAlternative(rfc, winningAlt);
            rfc.setAdr(adr);
        }
        // Case 2: closed without alternative (US-23)
        else {
            rfc.setStatus(RFC.Status.CLOSED_NON_DECIDED);
            // Puedes guardar el motivo de cierre si tu modelo lo soporta
        }

        RFC saved = rfcRepository.save(rfc);
        return toResponse(saved);
    }

}
