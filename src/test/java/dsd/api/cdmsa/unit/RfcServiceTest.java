package dsd.api.cdmsa.unit;

import dsd.api.cdmsa.assembler.ContextSummaryModelAssembler;
import dsd.api.cdmsa.assembler.UserSummaryModelAssembler;
import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.exception.*;
import dsd.api.cdmsa.model.*;
import dsd.api.cdmsa.model.event.RfcUpdatedEvent;
import dsd.api.cdmsa.repository.*;
import dsd.api.cdmsa.service.ContextService;
import dsd.api.cdmsa.service.IngestionService;
import dsd.api.cdmsa.service.RfcService;
import dsd.api.cdmsa.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RfcService.
 */
@ExtendWith(MockitoExtension.class)
class RfcServiceTest {

    @Mock
    private RfcRepository rfcRepository;

    @Mock
    private AdrRepository adrRepository;

    @Mock
    private AlternativeAttachmentRepository alternativeAttachmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RfcAttachmentRepository rfcAttachmentRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AlternativeRepository alternativeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private UserReviewerRepository userReviewerRepository;

    @Mock
    private UserObserverRepository userObserverRepository;

    @Mock
    private ContextReviewerRepository contextReviewerRepository;

    @Mock
    private UserService userService;

    @Mock
    private ContextService contextService;

    @Mock
    private IngestionService ingestionService;

    @Mock
    private UserSummaryModelAssembler userSummaryModelAssembler;

    @Mock
    private ContextSummaryModelAssembler contextSummaryModelAssembler;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RfcService rfcService;
    
    // ------------------------------------------------------------
    // createRfc
    // ------------------------------------------------------------


    @Test
    void createRfc_shouldCreateRfc_whenValidRequestAndDependenciesExist() throws IOException {
        Long userId = 1L;
        Long orgId = 10L;
        Long templateId = 5L;

        // DTO Update: (title, description, templateId, xml) - NO addition
        CreateRfcRequest request = new CreateRfcRequest(
                "  My RFC  ",
                "Desc",
                templateId,
                null);

        User author = new User(); author.setId(userId);
        Organization org = new Organization(); org.setId(orgId);
        Template template = new Template(); template.setId(templateId);

        RFC rfc = new RFC();
        rfc.setId(100L);
        rfc.setTitle("My RFC");
        rfc.setDescription("Desc");
        rfc.setUser(author);
        rfc.setTemplate(template);
        rfc.setOrg(org);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);

        // Mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(rfcRepository.save(any(RFC.class))).thenReturn(rfc);
        when(rfcRepository.findById(100L)).thenReturn(Optional.of(rfc));

        when(userService.findAllUsersByid(anyList(), eq(orgId))).thenReturn(List.of(author));
        
        // Mocks per la costruzione della risposta (allegati e commenti)
        when(commentRepository.findByRfcId(rfc.getId())).thenReturn(Collections.emptyList());
        when(rfcAttachmentRepository.findByRfcId(rfc.getId())).thenReturn(Collections.emptyList());

        // Esecuzione (passiamo null ai files per simulare assenza allegati)
        RfcResponse response = rfcService.createRfc(userId, orgId, request, null);

        assertNotNull(response);

        // Verifica trimming titolo
        ArgumentCaptor<RFC> captor = ArgumentCaptor.forClass(RFC.class);
        verify(rfcRepository).save(captor.capture());
        assertEquals("My RFC", captor.getValue().getTitle());
        verify(ingestionService).ingestRFC(any(RFC.class));
        verify(userReviewerRepository).saveAll(anyList());
    }

    @Test
    void createRfc_shouldThrowBadRequest_whenRequestIsNull() {
        assertThrows(RfcBadRequestException.class,
                () -> rfcService.createRfc(1L, 10L, null, null));
    }

    @Test
    void createRfc_shouldThrowBadRequest_whenTitleIsBlank() {
        CreateRfcRequest request = new CreateRfcRequest(
                "  ",
                "Desc",
                1L,
                null);

        assertThrows(RfcBadRequestException.class,
                () -> rfcService.createRfc(1L, 10L, request, null));
    }


    // ------------------------------------------------------------
    // getRfcById / listRfcs / listRfcsByOrg
    // ------------------------------------------------------------

    @Test
    void getRfcById_shouldReturnRfc_whenExists() {
        RFC rfc = new RFC();
        rfc.setId(100L);
        rfc.setTitle("RFC title");

        when(rfcRepository.findById(100L)).thenReturn(Optional.of(rfc));
     // when(alternativeRepository.findByRfcId(100L)).thenReturn(Collections.emptyList()); // Unecessary Stubbing
    //  when(commentRepository.findByRfcId(100L)).thenReturn(Collections.emptyList());

        RFC response = rfcService.getRfcById(100L);

        assertNotNull(response);
        verify(rfcRepository).findById(100L);
    }
        

    @Test
    void getRfcById_shouldThrowNotFound_whenDoesNotExist() {
        when(rfcRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RfcNotFoundException.class,
                () -> rfcService.getRfcById(999L));
    }

    @Test
    void listRfcs_shouldCallRepositoryFindAll() {
        RFC rfc = new RFC();
        rfc.setId(1L);

        Page<RFC> page = new PageImpl<>(List.of(rfc));
        Pageable pageable = PageRequest.of(0, 10);

        when(rfcRepository.findAll(pageable)).thenReturn(page);
        when(commentRepository.findByRfcId(rfc.getId())).thenReturn(Collections.emptyList());

        Page<RfcResponse> result = rfcService.listRfcs(pageable);

        assertEquals(1, result.getTotalElements());
        verify(rfcRepository).findAll(pageable);
    }

    @Test
    void listRfcsByOrg_shouldCallRepositoryFindByOrgId() {
        RFC rfc = new RFC();
        rfc.setId(1L);

        Page<RFC> page = new PageImpl<>(List.of(rfc));
        Pageable pageable = PageRequest.of(0, 10);

        when(rfcRepository.findByOrgId(10L, pageable)).thenReturn(page);
        when(commentRepository.findByRfcId(rfc.getId())).thenReturn(Collections.emptyList());

        Page<RfcResponse> result = rfcService.listRfcsByOrg(10L, pageable);

        assertEquals(1, result.getTotalElements());
        verify(rfcRepository).findByOrgId(10L, pageable);
    }

    // ------------------------------------------------------------
    // postCommentToRfc
    // ------------------------------------------------------------

    @Test
    void postCommentToRfc_shouldThrowBadRequest_whenRfcClosed() {
        Long rfcId = 100L;
        Long userId = 1L;
        Long orgId = 1L;

        Organization org = new Organization();
        org.setId(orgId);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.CLOSED_DECIDED);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        CreateCommentRequest request = new CreateCommentRequest("comment", null, null);

        assertThrows(RfcBadRequestException.class,
              () -> rfcService.postCommentToRfc(rfcId, user, orgId, request));
    }

    @Test
    void postCommentToRfc_shouldCreateComment_whenUnderReview() {
        Long rfcId = 100L;
        Long userId = 1L;
        Long orgId = 1L;

        Organization org = new Organization();
        org.setId(orgId);

        User user = new User();
        user.setId(userId);
        user.setOrg(org);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);

        User author = new User();
        author.setId(userId);

        CreateCommentRequest request = new CreateCommentRequest("Nice RFC", null, null);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));
        // when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(commentRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());
        when(alternativeRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());

       RfcResponse response = rfcService.postCommentToRfc(rfcId, user, orgId, request);

        assertNotNull(response);
        verify(commentRepository).save(any(Comment.class));
        verify(ingestionService).ingestRFC(rfc);
    }

    // ------------------------------------------------------------
    // addAlternative
    // ------------------------------------------------------------

    @Test
    void addAlternative_shouldThrowBadRequest_whenRequestNull() {
        assertThrows(RfcAlternativeBadRequestException.class,
                () -> rfcService.addAlternative(1L, 1L, null, null));
    }

    @Test
    void addAlternative_shouldThrowInvalidStatus_whenRfcNotUnderReview() {
        Long rfcId = 1L;
        Long userId = 1L;

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.CLOSED_DECIDED);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        CreateAlternativeRequest request = new CreateAlternativeRequest("Alt", "Desc", "Pros", "Cons", null);

        assertThrows(RfcInvalidStatusException.class,
                () -> rfcService.addAlternative(rfcId, userId, request, null));
    }

    @Test
    void addAlternative_shouldThrowNotAllowed_whenUserIsNotAuthor() {
        Long rfcId = 1L;
        Long userId = 2L; // not author

        User author = new User();
        author.setId(1L);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);
        rfc.setUser(author);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        CreateAlternativeRequest request = new CreateAlternativeRequest("Alt", "Desc", "Pros", "Cons", null);

        assertThrows(RfcAlternativeNotAllowedException.class,
                () -> rfcService.addAlternative(rfcId, userId, request, null));
    }

    @Test
    void addAlternative_shouldCreateAlternative_whenAuthorAndUnderReview() throws IOException {
        Long rfcId = 1L;
        Long userId = 1L;
        Long orgId = 10L;
        Long generatedAltId = 55L;

        User author = new User();
        author.setId(userId);

        Organization org = new Organization();
        org.setId(orgId);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);
        rfc.setUser(author);
        rfc.setOrg(org);

        CreateAlternativeRequest request = new CreateAlternativeRequest("Alt Title", "Desc", "Pros", "Cons", null);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        when(alternativeRepository.save(any(Alternative.class)))
                .thenAnswer(invocation -> {
                    Alternative a = invocation.getArgument(0);
                    a.setId(generatedAltId);
                    return a;
                });

        when(rfcRepository.save(any(RFC.class))).thenAnswer(i -> i.getArgument(0));

        when(alternativeRepository.findById(generatedAltId)).thenAnswer(inv -> {
            Alternative a = new Alternative();
            a.setId(generatedAltId);
            a.setRfc(rfc);
            a.setAuthor(author);
            a.setTitle("Alt Title");
            return Optional.of(a);
        });

        when(voteRepository.countByAlternativeAndOutcome(any(), eq(true))).thenReturn(0);
        when(voteRepository.countByAlternativeAndOutcome(any(), eq(false))).thenReturn(0);
        when(alternativeAttachmentRepository.findByAlternativeId(generatedAltId)).thenReturn(Collections.emptyList());

        AlternativeSpecificResponse response = rfcService.addAlternative(rfcId, userId, request, null);

        assertNotNull(response);
        assertEquals("Alt Title", response.title());

        verify(alternativeRepository).save(any(Alternative.class));
        verify(rfcRepository).save(any(RFC.class));
        verify(ingestionService).ingestRFC(any(RFC.class));
    }


    // ------------------------------------------------------------
    // closeRfc
    // ------------------------------------------------------------

    @Test
    void closeRfc_shouldThrowInvalidStatus_whenNotUnderReview() {
        Long rfcId = 1L;
        Long userId = 1L;

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.CLOSED_DECIDED);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        CloseRfcRequest request = new CloseRfcRequest(null, null);

        assertThrows(RfcInvalidStatusException.class,
                () -> rfcService.closeRfc(rfcId, userId, request));
    }

    @Test
    void closeRfc_shouldThrowNotAllowed_whenUserNotAuthor() {
        Long rfcId = 1L;
        Long userId = 2L; // not author

        User author = new User();
        author.setId(1L);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);
        rfc.setUser(author);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        CloseRfcRequest request = new CloseRfcRequest(null, null);

        assertThrows(RfcAlternativeNotAllowedException.class,
                () -> rfcService.closeRfc(rfcId, userId, request));
    }

    @Test
    void closeRfc_shouldCloseWithoutAlternative_whenRequestHasNoAltId() {
        Long rfcId = 1L;
        Long userId = 1L;
        User author = new User(); author.setId(userId);
        Organization org = new Organization(); org.setId(10L);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);
        rfc.setUser(author);
        rfc.setOrg(org);

        rfc.setObservers(new java.util.HashSet<>());

        // Mocks
        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        when(rfcRepository.save(any(RFC.class))).thenAnswer(i -> i.getArgument(0));

        when(commentRepository.findByRfcId(rfcId)).thenReturn(java.util.Collections.emptyList());

        RfcResponse response = rfcService.closeRfc(rfcId, userId, new CloseRfcRequest(null, null));

        assertEquals(RFC.Status.CLOSED_NON_DECIDED, response.status());
        verify(ingestionService).ingestRFC(any(RFC.class));
        verify(eventPublisher).publishEvent(any(RfcUpdatedEvent.class));
    }

    // ------------------------------------------------------------
    // voteForAlternative
    // ------------------------------------------------------------

    @Test
    void voteForAlternative_shouldCreateNewVote_whenNoExistingVote() {
        Long altId = 1L;
        Long userId = 1L;

        Alternative alternative = new Alternative();
        alternative.setId(altId);

        User voter = new User();
        voter.setId(userId);

        when(alternativeRepository.findById(altId)).thenReturn(Optional.of(alternative));
        when(userRepository.findById(userId)).thenReturn(Optional.of(voter));
        when(voteRepository.findByAlternativeAndVoter(alternative, voter))
                .thenReturn(Optional.empty());

        when(voteRepository.countByAlternativeAndOutcome(alternative, true)).thenReturn(1);
        when(voteRepository.countByAlternativeAndOutcome(alternative, false)).thenReturn(0);

        VoteResponse response = rfcService.voteForAlternative(altId, userId, true);

        assertEquals(1, response.yesCount());
        assertEquals(0, response.noCount());
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    void voteForAlternative_shouldDeleteVote_whenClickSameOutcomeAgain() {
        Long altId = 1L;
        Long userId = 1L;

        Alternative alternative = new Alternative();
        alternative.setId(altId);

        User voter = new User();
        voter.setId(userId);

        Vote existing = new Vote();
        existing.setAlternative(alternative);
        existing.setVoter(voter);
        existing.setOutcome(true);

        when(alternativeRepository.findById(altId)).thenReturn(Optional.of(alternative));
        when(userRepository.findById(userId)).thenReturn(Optional.of(voter));
        when(voteRepository.findByAlternativeAndVoter(alternative, voter))
                .thenReturn(Optional.of(existing));

        when(voteRepository.countByAlternativeAndOutcome(alternative, true)).thenReturn(0);
        when(voteRepository.countByAlternativeAndOutcome(alternative, false)).thenReturn(0);

        VoteResponse response = rfcService.voteForAlternative(altId, userId, true);

        assertEquals(0, response.yesCount());
        assertEquals(0, response.noCount());
        verify(voteRepository).delete(existing);
    }

    @Test
    void updateOrCreateDiagram_shouldUpdateXml_whenAuthor() {
        Long rfcId = 100L;
        Long userId = 1L;
        String newXml = "<xml>Diagram</xml>";

        User author = new User(); author.setId(userId);
        Organization org = new Organization(); org.setId(5L);
        RFC rfc = new RFC();
        Context context  = new Context();
        ContextReviewer cr = new ContextReviewer();
        cr.setContext(context);

        rfc.setId(rfcId);
        rfc.setUser(author);
        rfc.setOrg(org);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));
        // Mock per la risposta
        when(rfcRepository.findByIdAndOrgId(rfcId, 5L)).thenReturn(Optional.of(rfc));
        when(rfcAttachmentRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());
        when(commentRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());
        when(alternativeRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());
        when(userObserverRepository.existsById(any(UserRFCId.class))).thenReturn(false);
        when(contextReviewerRepository.findAllByRfc(rfc)).thenReturn(List.of(cr));
        when(contextSummaryModelAssembler.toModel(context)).thenReturn(null);

        rfcService.updateOrCreateDiagram(rfcId, userId, newXml);

        assertEquals(newXml, rfc.getXml());
        verify(rfcRepository).save(rfc);
    }

    @Test
    void updateOrCreateAlternativeDiagram_shouldUpdateXml() {
        Long altId = 200L;
        Long userId = 1L;
        String newXml = "<xml>AltDiagram</xml>";

        User author = new User(); author.setId(userId);
        Organization org = new Organization(); org.setId(5L);
        RFC rfc = new RFC(); rfc.setOrg(org); rfc.setStatus(RFC.Status.UNDER_REVIEW);

        Alternative alt = new Alternative();
        alt.setId(altId);
        alt.setAuthor(author);
        alt.setRfc(rfc);

        when(alternativeRepository.findById(altId)).thenReturn(Optional.of(alt));
        when(alternativeAttachmentRepository.findByAlternativeId(altId)).thenReturn(Collections.emptyList());

        AlternativeSpecificResponse response = rfcService.updateOrCreateAlternativeDiagram(altId, userId, newXml);

        assertEquals(newXml, alt.getXml());
        verify(alternativeRepository).save(alt);
    }

    // =========================================================================
    // 4. ATTACHMENT UPLOAD/DOWNLOAD (New)
    // =========================================================================

    @Test
    void uploadMultipleAttachments_shouldSaveFiles_RFC() throws IOException {
        Long rfcId = 100L;
        Long userId = 1L;
        RFC rfc = new RFC(); rfc.setId(rfcId);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        MockMultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "data".getBytes());

        List<RfcAttachment> result = rfcService.uploadMultipleAttachments(rfcId, userId, List.of(file));

        assertEquals(1, result.size());
        verify(rfcAttachmentRepository).save(any(RfcAttachment.class));
    }

    @Test
    void downloadRfcAttachment_shouldReturnResource_whenAuthorized(@TempDir Path tempDir) throws IOException {
        // Creiamo un file temporaneo reale per evitare errori di 'file not found'
        Path tempFile = Files.createFile(tempDir.resolve("test.txt"));
        Files.writeString(tempFile, "Content");

        Long attId = 10L;
        Long userOrgId = 50L;

        Organization org = new Organization(); org.setId(userOrgId);
        RFC rfc = new RFC(); rfc.setOrg(org);

        RfcAttachment attachment = new RfcAttachment();
        attachment.setId(attId);
        attachment.setRfc(rfc);
        attachment.setFileName("test.txt");
        attachment.setFilePath(tempFile.toAbsolutePath().toString());

        when(rfcAttachmentRepository.findById(attId)).thenReturn(Optional.of(attachment));

        RfcService.FileDownloadDTO result = rfcService.downloadRfcAttachment(attId, userOrgId);

        assertNotNull(result.resource());
        assertTrue(result.resource().exists());
    }

    @Test
    void downloadRfcAttachment_shouldThrowForbidden_whenWrongOrg() {
        Long attId = 10L;
        Long userOrgId = 99L; // Wrong ID

        Organization org = new Organization(); org.setId(50L);
        RFC rfc = new RFC(); rfc.setOrg(org);
        RfcAttachment attachment = new RfcAttachment();
        attachment.setId(attId);
        attachment.setRfc(rfc);

        when(rfcAttachmentRepository.findById(attId)).thenReturn(Optional.of(attachment));

        assertThrows(RfcAlternativeNotAllowedException.class,
                () -> rfcService.downloadRfcAttachment(attId, userOrgId));
    }

    // =========================================================================
    // 5. UPDATE TEXT FIELDS (Including 'Addition')
    // =========================================================================

    @Test
    void updateRfcText_shouldUpdateAddition_whenProvided() {
        Long rfcId = 100L;
        Long userId = 1L;
        Long orgId = 10L;

        User author = new User(); author.setId(userId);
        Organization org = new Organization(); org.setId(orgId);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setUser(author);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);
        rfc.setOrg(org);

        UpdateRfcRequest request = new UpdateRfcRequest("New Title", "New Desc", "My Addition Note");

        // Mock Find
        when(rfcRepository.findByIdAndOrgId(rfcId, orgId)).thenReturn(Optional.of(rfc));

        when(rfcRepository.save(any(RFC.class))).thenAnswer(i -> i.getArgument(0));

        when(rfcAttachmentRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());
        when(commentRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());
        when(alternativeRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());

        when(userObserverRepository.existsById(any(UserRFCId.class))).thenReturn(false);
        when(userReviewerRepository.findAllByRfc(rfc)).thenReturn(Collections.emptyList());
        when(contextReviewerRepository.findAllByRfc(rfc)).thenReturn(Collections.emptyList());

        rfcService.updateRfcText(rfcId, userId, orgId, request);

        ArgumentCaptor<RFC> captor = ArgumentCaptor.forClass(RFC.class);
        verify(rfcRepository).save(captor.capture());

        RFC saved = captor.getValue();
        assertEquals("New Title", saved.getTitle());
        assertEquals("My Addition Note", saved.getAddition());

        verify(ingestionService).ingestRFC(saved);
    }

    @Test
    void updateAlternative_shouldUpdateAddition_whenProvided() {
        Long altId = 200L;
        Long userId = 1L;

        User author = new User(); author.setId(userId);
        Organization org = new Organization(); org.setId(5L);
        RFC rfc = new RFC();
        rfc.setOrg(org);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);

        Alternative alt = new Alternative();
        alt.setId(altId);
        alt.setAuthor(author);
        alt.setRfc(rfc);

        // UpdateRequest: (title, description, pros, cons, ADDITION)
        UpdateAlternativeRequest request = new UpdateAlternativeRequest(
                "T", "D", "P", "C", "Alt Addition");

        when(alternativeRepository.findById(altId)).thenReturn(Optional.of(alt));
        when(rfcRepository.save(any(RFC.class))).thenAnswer(i -> i.getArgument(0));
        when(alternativeAttachmentRepository.findByAlternativeId(altId)).thenReturn(Collections.emptyList());

        AlternativeSpecificResponse response = rfcService.updateAlternative(altId, userId, request);

        ArgumentCaptor<Alternative> captor = ArgumentCaptor.forClass(Alternative.class);
        verify(alternativeRepository).save(captor.capture());

        assertEquals("Alt Addition", captor.getValue().getAddition());
        verify(ingestionService).ingestRFC(rfc);
    }
}
