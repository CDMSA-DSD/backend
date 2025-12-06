package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.model.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import dsd.api.cdmsa.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dsd.api.cdmsa.exception.RfcAlternativeBadRequestException;
import dsd.api.cdmsa.exception.RfcAlternativeNotAllowedException;
import dsd.api.cdmsa.exception.RfcBadRequestException;
import dsd.api.cdmsa.exception.RfcDependencyNotFoundException;
import dsd.api.cdmsa.exception.RfcInvalidStatusException;
import dsd.api.cdmsa.exception.RfcNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class RfcService {
    // Existing repositories
    private final RfcRepository rfcRepository;
    private final AdrRepository adrRepository;
    private final UserRepository userRepository;
    private final TemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;
    private final AlternativeRepository alternativeRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final RfcAttachmentRepository rfcAttachmentRepository;
    private final AlternativeAttachmentRepository alternativeAttachmentRepository;

    // Folder where to upload attachments (all inside here right now)
    private final String UPLOAD_DIR = "C:\\Users\\carlo\\OneDrive\\Desktop\\uploads\\";

    @Transactional
    public RfcResponse postCommentToRfc(Long rfcId, Long userId, CreateCommentRequest request) {
        // in the future we should check that the user must be a reviewer of the RFC in
        // order to post a comment
        // ...

        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found"));

        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcBadRequestException("Comments are not allowed on closed RFCs");
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RfcNotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        comment.setRfc(rfc);

        // IF parentId is not null, add a parent-child relationship
        if (request.parentId() != null) {
            Comment parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new RfcNotFoundException("Parent comment not found"));

            // Validación extra: el parent debe pertenecer al mismo RFC
            if (!parent.getRfc().getId().equals(rfcId)) {
                throw new RfcBadRequestException("Reply comment must belong to the same RFC");
            }

            comment.setParent(parent);
        }

        commentRepository.save(comment);

        return getRfcById(rfcId);
    }

    // ---------- US-12: Create RFC ----------

    @Transactional
    public RfcResponse createRfc(Long userId, Long orgId, CreateRfcRequest request, List<MultipartFile> files) throws IOException {

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
        if (request.xml() != null) {
            rfc.setXml(request.xml().trim());
        }
        // Status defaults to UNDER_REVIEW in RFC model

        // Persist and map
        RFC saved = rfcRepository.save(rfc);

        // Check attachments presence
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                // Saving the uploaded files
                saveAttachmentInternal(saved, file);
            }
        }

        // Getting the just upload files
        List<RfcAttachment> freshAttachments = rfcAttachmentRepository.findByRfcId(saved.getId());

        if (freshAttachments == null || freshAttachments.isEmpty()) {
            // Case A: no attachments
            return toResponse(saved);
        } else {
            // Case B: attachments
            return toResponse(saved, freshAttachments);
        }
    }


    private RfcResponse toResponse(RFC rfc, List<RfcAttachment> attachments) {
        int count = commentRepository.findByRfcId(rfc.getId()).size();

        List<AttachmentResponse> attachmentDtos = attachments.stream()
                .map(this::toAttachmentDto)
                .toList();

        return new RfcResponse(
                rfc.getId(),
                rfc.getTitle(),
                rfc.getDescription(),
                rfc.getUser() != null ? rfc.getUser().getId() : null,
                rfc.getUser() != null ? rfc.getUser().getFirstname() : null,
                rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
                rfc.getOrg() != null ? rfc.getOrg().getId() : null,
                rfc.getStatus(),
                rfc.getAddition(),
                rfc.getCreatedAt(),
                rfc.getUpdatedAt(),
                rfc.getXml(),
                (long) count,
                List.of(), // alternatives summary
                List.of(), // comments summary
                attachmentDtos // returning also the list of attachments
        );
    }

    private RfcAttachment saveAttachmentInternal(RFC rfc, MultipartFile file) throws IOException {

        // Clean filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new IOException("Filename contains invalid path sequence " + originalFileName);
        }

        // Create folder where to save (if not present already)
        String uniqueFileName = System.currentTimeMillis() + "_" + originalFileName;
        String fullPath = UPLOAD_DIR + uniqueFileName;

        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) directory.mkdirs();

        // Writing
        file.transferTo(new File(fullPath));

        // Saving in the database the metadata of the attachment
        RfcAttachment att = new RfcAttachment();
        att.setRfc(rfc);
        att.setFileName(originalFileName);
        att.setFilePath(fullPath);
        att.setContentType(file.getContentType());
        att.setSize(file.getSize());

        return rfcAttachmentRepository.save(att);
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
    public RfcSpecificResponse getRfcByIdForOrg(Long rfcId, Long orgId, Long userId) {

        RFC rfc = rfcRepository.findByIdAndOrgId(rfcId, orgId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));
        RfcResponse detailedRfc = toDetailedResponse(rfc);

        // Determine if the requesting user is the author of the RFC
        boolean isAuthor = rfc.getUser().getId().equals(userId);
        return new RfcSpecificResponse(
            rfc.getId(),
            rfc.getTitle(),
            rfc.getDescription(),
            rfc.getUser() != null ? rfc.getUser().getId() : null,
            rfc.getUser() != null ? rfc.getUser().getFirstname() : null, //Before getName
            rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
            rfc.getOrg() != null ? rfc.getOrg().getId() : null,
            rfc.getStatus(),
            rfc.getAddition(),
            rfc.getCreatedAt(),
            rfc.getUpdatedAt(),
            rfc.getXml(),
            isAuthor,
            detailedRfc.alternatives(),
            detailedRfc.comments(),
            detailedRfc.attachments());
    }

    private RfcResponse toResponse(RFC rfc) {
        // Compute comment count (may load comments; acceptable for page sizes)
        int count = commentRepository.findByRfcId(rfc.getId()).size();
        return new RfcResponse(
                rfc.getId(),
                rfc.getTitle(),
                rfc.getDescription(),
                rfc.getUser() != null ? rfc.getUser().getId() : null,
                rfc.getUser() != null ? rfc.getUser().getFirstname() : null, //Before getName
                rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
                rfc.getOrg() != null ? rfc.getOrg().getId() : null,
                rfc.getStatus(),
                rfc.getAddition(),
                rfc.getCreatedAt(),
                rfc.getUpdatedAt(),
                rfc.getXml(),
                (long) count,
                java.util.List.of(), // lightweight list for summary
                java.util.List.of(),
                java.util.List.of());
    }

    private RfcResponse toDetailedResponse(RFC rfc) {

        List<AlternativeResponse> alts = alternativeRepository.findByRfcId(rfc.getId()).stream()
                .map(alt -> {
                    int yesCount = voteRepository.countByAlternativeAndOutcome(alt, true);
                    int noCount = voteRepository.countByAlternativeAndOutcome(alt, false);
                    return AlternativeResponse.fromEntityVotes(alt, yesCount, noCount);
                })
                .collect(Collectors.toList());

        List<Comment> comments = commentRepository.findByRfcId(rfc.getId());

        // Map: parentId → children list
        Map<Long, List<Comment>> childrenMap = comments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // Comments without parent (root comments)
        List<Comment> roots = comments.stream()
                .filter(c -> c.getParent() == null)
                .toList();

        // Transform it into a thread
        List<CommentResponse> threaded = roots.stream()
                .map(root -> buildThreaded(root, childrenMap))
                .toList();

        List<RfcAttachment> attachments = rfcAttachmentRepository.findByRfcId(rfc.getId());
        List<AttachmentResponse> attachmentDtos = attachments.stream()
                .map(this::toAttachmentDto)
                .toList();

        return new RfcResponse(
                rfc.getId(),
                rfc.getTitle(),
                rfc.getDescription(),
                rfc.getUser() != null ? rfc.getUser().getId() : null,
                rfc.getUser() != null ? rfc.getUser().getFirstname() : null, // Before getName
                rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
                rfc.getOrg() != null ? rfc.getOrg().getId() : null,
                rfc.getStatus(),
                rfc.getAddition(),
                rfc.getCreatedAt(),
                rfc.getUpdatedAt(),
                rfc.getXml(),
                (long) comments.size(),
                alts,
                threaded,
                attachmentDtos);
    }

    private AttachmentResponse toAttachmentDto(RfcAttachment att) {
        return new AttachmentResponse(
                att.getId(),
                att.getFileName(),
                att.getContentType(),
                att.getSize(),
                "/rfcs/attachments/download/RFC/" + att.getId()
        );
    }

    private CommentResponse buildThreaded(Comment comment, Map<Long, List<Comment>> childrenMap) {

        List<CommentResponse> replies = childrenMap.getOrDefault(comment.getId(), List.of())
                .stream()
                .map(child -> buildThreaded(child, childrenMap))
                .toList();

        return CommentResponse.fromEntity(comment, replies);
    }

    // ---------- Alternatives (POST & GET) ----------


    @Transactional(readOnly = true)
    public List<AlternativeResponse> listAlternatives(Long rfcId) {

        // Ensure RFC exists (helps return 404 instead of empty list for invalid id)
        if (!rfcRepository.existsById(rfcId)) {
            throw new RfcNotFoundException("RFC not found with id " + rfcId);
        }

        return alternativeRepository.findByRfcId(rfcId).stream()
                .map(alt -> {
                    int yesCount = voteRepository.countByAlternativeAndOutcome(alt, true);
                    int noCount = voteRepository.countByAlternativeAndOutcome(alt, false);
                    return AlternativeResponse.fromEntityVotes(alt, yesCount, noCount);
                })
                .collect(Collectors.toList());
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
                .map(alt -> {
                    int yesCount = voteRepository.countByAlternativeAndOutcome(alt, true);
                    int noCount = voteRepository.countByAlternativeAndOutcome(alt, false);
                    return AlternativeResponse.fromEntityVotes(alt, yesCount, noCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public RfcResponse closeRfc(Long rfcId, Long userId, CloseRfcRequest request) {
        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));

        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException("RFC is not under review");
        }

        if (!Objects.equals(rfc.getUser().getId(), userId)) {
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
            // rfc.setWinningAlternative(winningAlt); maybe cool to have?

            ADR adr = adrRepository.findByRfcId(rfcId)
                    .orElseThrow(() -> new RfcNotFoundException("ADR not found for this RFC"));

            rfc.setAdr(adr);
        }
        // Case 2: closed without alternative (US-23)
        else {
            rfc.setStatus(RFC.Status.CLOSED_NON_DECIDED);

        }

        RFC saved = rfcRepository.save(rfc);
        return toResponse(saved);
    }

    @Transactional
    public VoteResponse voteForAlternative(Long altId, Long userId, boolean outcome) {
        Alternative alternative = alternativeRepository.findById(altId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative not found"));

        User voter = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Optional<Vote> existing = voteRepository.findByAlternativeAndVoter(alternative, voter);

        if (existing.isPresent()) {
            Vote vote = existing.get();

            // delete vote if u click on the same thumb
            if (vote.getOutcome() != null && vote.getOutcome().equals(outcome)) {
                voteRepository.delete(vote);
                int yesCount = voteRepository.countByAlternativeAndOutcome(alternative, true);
                int noCount = voteRepository.countByAlternativeAndOutcome(alternative, false);

                return new VoteResponse(yesCount, noCount);
            }

            // change vote instead
            vote.setOutcome(outcome);
            voteRepository.save(vote);
            int yesCount = voteRepository.countByAlternativeAndOutcome(alternative, true);
            int noCount = voteRepository.countByAlternativeAndOutcome(alternative, false);

            return new VoteResponse(yesCount, noCount);
        }

        // no existing vote
        Vote newVote = new Vote();
        newVote.setAlternative(alternative);
        newVote.setVoter(voter);
        newVote.setOutcome(outcome);
        voteRepository.save(newVote);
        int yesCount = voteRepository.countByAlternativeAndOutcome(alternative, true);
        int noCount = voteRepository.countByAlternativeAndOutcome(alternative, false);
        return new VoteResponse(yesCount, noCount);
    }

    @Transactional
    public RfcSpecificResponse updateOrCreateDiagram(Long rfcId, Long userId, String xmlContent) {

        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found"));

        // Check author
        if (!rfc.getUser().getId().equals(userId)) {
            throw new RfcAlternativeNotAllowedException("Only the author can edit the diagram");
        }

        // Check status
        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException("Cannot edit diagram of a closed RFC");
        }

        rfc.setXml(xmlContent != null ? xmlContent.trim() : null);

        rfcRepository.save(rfc);
        return getRfcByIdForOrg(rfcId, rfc.getOrg().getId(), userId);
    }



    @Transactional
    public List<RfcAttachment> uploadMultipleAttachments(Long rfcId, Long userId, List<MultipartFile> files) throws IOException {

        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        List<RfcAttachment> savedAttachments = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            RfcAttachment saved = saveAttachmentInternal(rfc, file);
            savedAttachments.add(saved);
        }

        return savedAttachments;
    }

    // probably not needed
    @Transactional(readOnly = true)
    public List<RfcAttachment> getRfcAttachments(Long rfcId) {

        if (!rfcRepository.existsById(rfcId)) {
            throw new RfcNotFoundException("RFC not found with id " + rfcId);
        }

        return rfcAttachmentRepository.findByRfcId(rfcId);
    }

    // DTO interno per passare i dati al Controller in modo pulito
    public record FileDownloadDTO(Resource resource, String fileName, String contentType) {}

    @Transactional(readOnly = true)
    public FileDownloadDTO downloadRfcAttachment(Long attachmentId, Long userOrgId) {

        // Get metadata from the database
        RfcAttachment attachment = rfcAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found with id " + attachmentId));

        Long resourceOrgId = attachment.getRfc().getOrg().getId();

        if (!resourceOrgId.equals(userOrgId)) {
            throw new RfcAlternativeNotAllowedException("You are not authorized to download this file.");
        }


        try {
            // Retrieve file from disk
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            // Checks
            if (resource.exists() && resource.isReadable()) {
                return new FileDownloadDTO(
                        resource,
                        attachment.getFileName(),
                        attachment.getContentType()
                );
            } else {
                throw new RuntimeException("File not found or not readable on disk: " + attachment.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Transactional
    public RfcSpecificResponse updateRfcText(Long rfcId, Long userId, Long orgId, UpdateRfcRequest request) {

        RFC rfc = rfcRepository.findByIdAndOrgId(rfcId, orgId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));

        if (!rfc.getUser().getId().equals(userId)) {
            throw new RfcAlternativeNotAllowedException("Only the author can edit the RFC details");
        }

        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException("Cannot edit an RFC that is closed");
        }

        rfc.setTitle(request.title().trim());
        rfc.setDescription(request.description().trim());
        if (request.addition() != null) {
            rfc.setAddition(request.addition().trim());
        }

        rfcRepository.save(rfc);

        return getRfcByIdForOrg(rfcId, orgId, userId);
    }


    @Transactional
    public AlternativeSpecificResponse addAlternative(Long rfcId, Long userId, CreateAlternativeRequest request, List<MultipartFile> files) throws IOException {

        if (request == null) throw new RfcAlternativeBadRequestException("Request body cannot be null");
        if (request.title() == null || request.title().isBlank()) throw new RfcAlternativeBadRequestException("Request title cannot be null");
        if (request.description() == null || request.description().isBlank()) throw new RfcAlternativeBadRequestException("Request description cannot be null");

        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));

        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException("Alternatives can only be created for RFCs in UNDER_REVIEW status");
        }
        if (!rfc.getUser().getId().equals(userId)) {
            throw new RfcAlternativeNotAllowedException("Only the author of the RFC is allowed to create alternatives");
        }

        Alternative alternative = new Alternative();
        alternative.setRfc(rfc);
        alternative.setTitle(request.title().trim());
        alternative.setDescription(request.description().trim());
        alternative.setAuthor(rfc.getUser());
        alternative.setPros(request.pros());
        alternative.setCons(request.cons());
        if (request.xml() != null) {
            alternative.setXml(request.xml().trim());
        }
        // addition will be set null by default

        Alternative saved = alternativeRepository.save(alternative);
        rfc.setUpdatedAt(java.time.Instant.now());
        rfcRepository.save(rfc);

        // Handle Files
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                saveAlternativeAttachmentInternal(saved, file);
            }
        }

        return getAlternativeByIdForOrg(saved.getId(), rfc.getOrg().getId(), userId);
    }

    @Transactional(readOnly = true)
    public AlternativeSpecificResponse getAlternativeByIdForOrg(Long altId, Long orgId, Long userId) {
        Alternative alt = alternativeRepository.findById(altId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative not found"));

        if (!alt.getRfc().getOrg().getId().equals(orgId)) {
            throw new RfcNotFoundException("Alternative not found in this organization");
        }

        int yesCount = voteRepository.countByAlternativeAndOutcome(alt, true);
        int noCount = voteRepository.countByAlternativeAndOutcome(alt, false);
        boolean isAuthor = alt.getAuthor().getId().equals(userId);

        List<AlternativeAttachment> attachments = alternativeAttachmentRepository.findByAlternativeId(altId);
        List<AttachmentResponse> attachmentDtos = attachments.stream()
                .map(this::toAlternativeAttachmentDto)
                .toList();

        return new AlternativeSpecificResponse(
                alt.getId(),
                alt.getTitle(),
                alt.getDescription(),
                alt.getPros(),
                alt.getCons(),
                alt.getXml(),
                alt.getAuthor().getId(),
                alt.getAuthor().getFirstname(),
                alt.getAddition(),
                alt.getCreatedAt(),
                alt.getUpdatedAt(),
                yesCount,
                noCount,
                isAuthor,
                attachmentDtos
        );
    }

    @Transactional
    public AlternativeSpecificResponse updateOrCreateAlternativeDiagram(Long altId, Long userId, String xmlContent) {
        Alternative alt = alternativeRepository.findById(altId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative not found"));

        if (!alt.getAuthor().getId().equals(userId)) {
            throw new RfcAlternativeNotAllowedException("Only the author can edit the diagram");
        }
        if (alt.getRfc().getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException("Cannot edit diagram of a closed RFC");
        }

        alt.setXml(xmlContent != null ? xmlContent.trim() : null);
        alternativeRepository.save(alt);

        return getAlternativeByIdForOrg(altId, alt.getRfc().getOrg().getId(), userId);
    }

    @Transactional
    public List<AlternativeAttachment> uploadMultipleAlternativeAttachments(Long altId, Long userId, List<MultipartFile> files) throws IOException {
        Alternative alt = alternativeRepository.findById(altId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative not found"));

        if (!alt.getAuthor().getId().equals(userId)) {
            throw new RfcAlternativeNotAllowedException("Only the author can upload files");
        }

        // Create folder if needed
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) directory.mkdirs();

        List<AlternativeAttachment> savedAttachments = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            savedAttachments.add(saveAlternativeAttachmentInternal(alt, file));
        }
        return savedAttachments;
    }

    @Transactional(readOnly = true)
    public FileDownloadDTO downloadAlternativeAttachment(Long attachmentId, Long userOrgId) {
        AlternativeAttachment attachment = alternativeAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));

        Long resourceOrgId = attachment.getAlternative().getRfc().getOrg().getId();
        if (!resourceOrgId.equals(userOrgId)) {
            throw new RfcAlternativeNotAllowedException("You are not authorized to download this file.");
        }

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return new FileDownloadDTO(resource, attachment.getFileName(), attachment.getContentType());
            } else {
                throw new RuntimeException("File not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    private AlternativeAttachment saveAlternativeAttachmentInternal(Alternative alt, MultipartFile file) throws IOException {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) throw new IOException("Invalid filename");

        String uniqueFileName = System.currentTimeMillis() + "_ALT_" + originalFileName;
        String fullPath = UPLOAD_DIR + uniqueFileName;

        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) directory.mkdirs();

        file.transferTo(new File(fullPath));

        AlternativeAttachment att = new AlternativeAttachment();
        att.setAlternative(alt);
        att.setFileName(originalFileName);
        att.setFilePath(fullPath);
        att.setContentType(file.getContentType());
        att.setSize(file.getSize());

        return alternativeAttachmentRepository.save(att);
    }

    private AttachmentResponse toAlternativeAttachmentDto(AlternativeAttachment att) {
        return new AttachmentResponse(
                att.getId(),
                att.getFileName(),
                att.getContentType(),
                att.getSize(),
                "/rfcs/alternatives/attachments/download/" + att.getId() // distinct URL
        );
    }

    @Transactional
    public AlternativeSpecificResponse updateAlternative(Long altId, Long userId, UpdateAlternativeRequest request) {
        Alternative alt = alternativeRepository.findById(altId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative not found"));

        RFC rfc = alt.getRfc();

        if (rfc.getStatus() != RFC.Status.UNDER_REVIEW) {
            throw new RfcInvalidStatusException("Cannot modify alternative when RFC is closed");
        }

        if (!alt.getAuthor().getId().equals(userId)) {
            throw new RfcAlternativeNotAllowedException("Only the author of the alternative can modify it");
        }

        if (request.title() != null) alt.setTitle(request.title().trim());
        if (request.description() != null) alt.setDescription(request.description().trim());
        if (request.pros() != null) alt.setPros(request.pros());
        if (request.cons() != null) alt.setCons(request.cons());
        if (request.addition() != null) {
            alt.setAddition(request.addition().trim());
        }

        alternativeRepository.save(alt);

        rfc.setUpdatedAt(java.time.Instant.now());
        rfcRepository.save(rfc);

        return getAlternativeByIdForOrg(altId, rfc.getOrg().getId(), userId);
    }

}
