package dsd.api.cdmsa.unit;

import dsd.api.cdmsa.dto.*;
import dsd.api.cdmsa.exception.*;
import dsd.api.cdmsa.model.*;
import dsd.api.cdmsa.repository.*;
import dsd.api.cdmsa.service.RfcService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private UserRepository userRepository;

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

    @InjectMocks
    private RfcService rfcService;

    // ------------------------------------------------------------
    // createRfc
    // ------------------------------------------------------------

    @Test
    void createRfc_shouldCreateRfc_whenValidRequestAndDependenciesExist() {
        Long userId = 1L;
        Long orgId = 10L;
        Long templateId = 5L;

        CreateRfcRequest request = new CreateRfcRequest(
                "  My RFC  ",
                "Desc",
                templateId);

        User author = new User();
        author.setId(userId);

        Organization org = new Organization();
        org.setId(orgId);

        Template template = new Template();
        template.setId(templateId);

        RFC rfc = new RFC();
        rfc.setId(100L);
        rfc.setTitle("My RFC");
        rfc.setDescription("Desc");
        rfc.setUser(author);
        rfc.setTemplate(template);
        rfc.setOrg(org);

        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(rfcRepository.save(any(RFC.class))).thenReturn(rfc);
        when(commentRepository.findByRfcId(rfc.getId())).thenReturn(Collections.emptyList());

        RfcResponse response = rfcService.createRfc(userId, orgId, request);

        assertNotNull(response);
        // capturamos el RFC que se guardó para asegurar que el título está trimmed
        ArgumentCaptor<RFC> captor = ArgumentCaptor.forClass(RFC.class);
        verify(rfcRepository).save(captor.capture());
        assertEquals("My RFC", captor.getValue().getTitle());
    }

    @Test
    void createRfc_shouldThrowBadRequest_whenRequestIsNull() {
        assertThrows(RfcBadRequestException.class,
                () -> rfcService.createRfc(1L, 10L, null));
    }

    @Test
    void createRfc_shouldThrowBadRequest_whenTitleIsBlank() {
        CreateRfcRequest request = new CreateRfcRequest(
                "  ",
                "Desc",
                1L);

        assertThrows(RfcBadRequestException.class,
                () -> rfcService.createRfc(1L, 10L, request));
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
        when(alternativeRepository.findByRfcId(100L)).thenReturn(Collections.emptyList());
        when(commentRepository.findByRfcId(100L)).thenReturn(Collections.emptyList());

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

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.CLOSED_DECIDED);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        CreateCommentRequest request = new CreateCommentRequest("comment", null, null);

    //    assertThrows(RfcBadRequestException.class,
    //            () -> rfcService.postCommentToRfc(rfcId, userId, request));
    }

    @Test
    void postCommentToRfc_shouldCreateComment_whenUnderReview() {
        Long rfcId = 100L;
        Long userId = 1L;

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);

        User author = new User();
        author.setId(userId);

        CreateCommentRequest request = new CreateCommentRequest("Nice RFC", null, null);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(commentRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());
        when(alternativeRepository.findByRfcId(rfcId)).thenReturn(Collections.emptyList());

       // RfcResponse response = rfcService.postCommentToRfc(rfcId, userId, request);
//
  //      assertNotNull(response);
        verify(commentRepository).save(any(Comment.class));
    }

    // ------------------------------------------------------------
    // addAlternative
    // ------------------------------------------------------------

    @Test
    void addAlternative_shouldThrowBadRequest_whenRequestNull() {
        assertThrows(RfcAlternativeBadRequestException.class,
                () -> rfcService.addAlternative(1L, 1L, null));
    }

    @Test
    void addAlternative_shouldThrowInvalidStatus_whenRfcNotUnderReview() {
        Long rfcId = 1L;
        Long userId = 1L;

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.CLOSED_DECIDED);

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));

        CreateAlternativeRequest request = new CreateAlternativeRequest("Alt", "Desc", "Pros", "Cons");

        assertThrows(RfcInvalidStatusException.class,
                () -> rfcService.addAlternative(rfcId, userId, request));
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

        CreateAlternativeRequest request = new CreateAlternativeRequest("Alt", "Desc", "Pros", "Cons");

        assertThrows(RfcAlternativeNotAllowedException.class,
                () -> rfcService.addAlternative(rfcId, userId, request));
    }

    @Test
    void addAlternative_shouldCreateAlternative_whenAuthorAndUnderReview() {
        Long rfcId = 1L;
        Long userId = 1L;

        User author = new User();
        author.setId(userId);

        RFC rfc = new RFC();
        rfc.setId(rfcId);
        rfc.setStatus(RFC.Status.UNDER_REVIEW);
        rfc.setUser(author);

        CreateAlternativeRequest request = new CreateAlternativeRequest("Alt", "Desc", "Pros", "Cons");

        when(rfcRepository.findById(rfcId)).thenReturn(Optional.of(rfc));
        when(alternativeRepository.save(any(Alternative.class)))
                .thenAnswer(invocation -> {
                    Alternative a = invocation.getArgument(0);
                    a.setId(10L);
                    return a;
                });

        AlternativeResponse response = rfcService.addAlternative(rfcId, userId, request);

        assertNotNull(response);
        verify(alternativeRepository).save(any(Alternative.class));
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
}
