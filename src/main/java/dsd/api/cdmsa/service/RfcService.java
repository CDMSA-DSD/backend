package dsd.api.cdmsa.service;

import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.model.*;

import java.util.List;
import java.util.Map;
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
    private final UserReviewerRepository userReviewerRepository;
    private final ContextReviewerRepository contextReviewerRepository;

    private final UserService userService;
    private final ContextService contextService;

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

        return toDetailedResponse(getRfcById(rfcId));
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
    public RFC getRfcById(Long rfcId) {
        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found with id " + rfcId));
        return rfc;
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
                rfc.getUser() != null ? rfc.getUser().getFirstname() : null, // Before getName
                rfc.getTemplate() != null ? rfc.getTemplate().getId() : null,
                rfc.getOrg() != null ? rfc.getOrg().getId() : null,
                rfc.getStatus(),
                rfc.getCreatedAt(),
                rfc.getUpdatedAt(),
                isAuthor,
                detailedRfc.alternatives(),
                detailedRfc.comments());
    }

    private RfcResponse toResponse(RFC rfc) {
        // Compute comment count (may load comments; acceptable for page sizes)
        int count = commentRepository.findByRfcId(rfc.getId()).size();
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
                (long) count,
                java.util.List.of(), // lightweight list for summary
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
                (long) comments.size(),
                alts,
                threaded);
    }

    private CommentResponse buildThreaded(Comment comment, Map<Long, List<Comment>> childrenMap) {

        List<CommentResponse> replies = childrenMap.getOrDefault(comment.getId(), List.of())
                .stream()
                .map(child -> buildThreaded(child, childrenMap))
                .toList();

        return CommentResponse.fromEntity(comment, replies);
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
    public void asignReviewersToRfc(ReviewersRequest reviewers, Long rfcId) {

        List<Long> userIds = reviewers.userIds();
        List<Long> contetxIds = reviewers.contextIds();
        RFC rfc = getRfcById(rfcId);

        if (!userIds.isEmpty()) {
            List<User> users = userService.findAllUsersByid(userIds);

            List<UserReviewer> userReviewers = users.stream()
                    .map(user -> {
                        UserRFCId id = new UserRFCId(user.getId(), rfcId);
                        UserReviewer userReviewer = new UserReviewer(id, user, rfc);

                        return userReviewer;
                    })
                    .toList();

            userReviewerRepository.saveAll(userReviewers);
        }

        if (!contetxIds.isEmpty()) {
            List<Context> contexts = contextService.findAllUsersByid(contetxIds);

            List<ContextReviewer> contextReviewers = contexts.stream()
                    .map(context -> {
                        ContextRFCId id = new ContextRFCId(context.getId(), rfcId);
                        ContextReviewer contextReviewer = new ContextReviewer(id, context, rfc);

                        return contextReviewer;
                    })
                    .toList();

            contextReviewerRepository.saveAll(contextReviewers);
        }

    }

}
