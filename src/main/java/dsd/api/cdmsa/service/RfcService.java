package dsd.api.cdmsa.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dsd.api.cdmsa.dto.CreateRfcRequest;
import dsd.api.cdmsa.dto.RfcResponse;
import dsd.api.cdmsa.exception.RfcBadRequestException;
import dsd.api.cdmsa.exception.RfcDependencyNotFoundException;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.repository.OrganizationRepository;
import dsd.api.cdmsa.repository.RfcRepository;
import dsd.api.cdmsa.repository.TemplateRepository;
import dsd.api.cdmsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RfcService {

    // Repositories injected via constructor (Lombok @RequiredArgsConstructor)
    private final RfcRepository rfcRepository;
    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Creates a new RFC and stores it in the database.
     *
     * Rules:
     * - title, description, templateId and orgId are required.
     * - RFC is created with status UNDER_REVIEW (default in RFC entity).
     * - User (author), Template and Organization must exist.
     *
     * @param userId  ID of the RFC author (current authenticated user)
     * @param request Payload with RFC creation data
     * @return RFCResponse DTO representing the created RFC
     */
    @Transactional
    public RfcResponse createRfc(Long userId, CreateRfcRequest request) {

        // --- Validate request body ---
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

        // --- Validate and load related entities ---

        // Author must exist
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RfcDependencyNotFoundException("User (author) not found"));

        // Template must exist
        var template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new RfcDependencyNotFoundException("Template not found"));

        // Organization must exist
        var org = organizationRepository.findById(request.orgId())
                .orElseThrow(() -> new RfcDependencyNotFoundException("Organization not found"));

        // --- Build RFC entity ---
        RFC rfc = new RFC();
        rfc.setTitle(request.title().trim());
        rfc.setDescription(request.description().trim());
        rfc.setTemplate(template);
        rfc.setOrg(org);
        rfc.setUser(user);
        // Status is set to UNDER_REVIEW by default in RFC model

        // --- Persist and map to response DTO ---
        RFC saved = rfcRepository.save(rfc);
        return toResponse(saved);
    }

    /**
     * Returns a paginated list of existing RFCs.
     *
     * @param pageable Pagination and sorting information
     * @return Page of RFCResponse DTOs
     */
    @Transactional(readOnly = true)
    public Page<RfcResponse> listRfcs(Pageable pageable) {
        return rfcRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * Maps an RFC entity to a RFCResponse DTO.
     * Keeps controllers decoupled from persistence details.
     */
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
}
