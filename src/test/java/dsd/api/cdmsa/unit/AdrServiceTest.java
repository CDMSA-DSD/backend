package dsd.api.cdmsa.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dsd.api.cdmsa.dto.AdrResponse;
import dsd.api.cdmsa.dto.AdrSpecificResponse;
import dsd.api.cdmsa.dto.CreateAdrRequest;
import dsd.api.cdmsa.dto.PublishAdrRequest;
import dsd.api.cdmsa.dto.UpdateAdrRequest;
import dsd.api.cdmsa.exception.RfcAlternativeNotAllowedException;
import dsd.api.cdmsa.exception.RfcInvalidStatusException;
import dsd.api.cdmsa.model.*;
import dsd.api.cdmsa.repository.AdrRepository;
import dsd.api.cdmsa.repository.AlternativeRepository;
import dsd.api.cdmsa.repository.RfcRepository;
import dsd.api.cdmsa.repository.UserRepository;
import dsd.api.cdmsa.service.AdrService;
import dsd.api.cdmsa.service.ContextService;
import dsd.api.cdmsa.service.IngestionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdrService Tests")
class AdrServiceTest {

    @Mock
    private AdrRepository adrRepository;

    @Mock
    private RfcRepository rfcRepository;

    @Mock
    private AlternativeRepository alternativeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AdrService adrService;

    @Mock
    private IngestionService ingestionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ContextService contextService;


    private RFC mockRfc;
    private ADR mockAdr;
    private User mockUser;
    private Organization mockOrg;
    private Alternative mockAlternative;

    @BeforeEach
    void setUp() {
        // Setup Organization (main fields)
        mockOrg = new Organization();
        mockOrg.setId(1L);
        mockOrg.setName("Test Organization");
        mockOrg.setGitHubToken("test-token");
        mockOrg.setRepoOwner("test-owner");
        mockOrg.setSelectedRepoName("test-repo");
        mockOrg.setSelectedBranchName("main");

        // Setup User (main fields)
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("testuser@example.com");
        mockUser.setFirstname("Test");
        mockUser.setLastname("User");
        mockUser.setPassword("encodedPassword123");
        mockUser.setOrg(mockOrg);

        // Setup RFC (main fields)
        mockRfc = new RFC();
        mockRfc.setId(1L);
        mockRfc.setTitle("Test RFC");
        mockRfc.setDescription("Test RFC Description");
        mockRfc.setUser(mockUser);
        mockRfc.setOrg(mockOrg);
        mockRfc.setCreatedAt(Instant.now());
        mockRfc.setUpdatedAt(Instant.now());

        // Setup Alternative (main fields)
        mockAlternative = new Alternative();
        mockAlternative.setId(2L);
        mockAlternative.setTitle("Strangler Pattern");
        mockAlternative.setDescription("Gradual migration strategy.");
        mockAlternative.setPros("Low risk, incremental.");
        mockAlternative.setCons("Slow process.");
        mockAlternative.setIsWinning(true);
        mockAlternative.setRfc(mockRfc);

        // Setup ADR (main fields)
        mockAdr = new ADR();
        mockAdr.setId(1L);
        mockAdr.setTitle("Test ADR");
        mockAdr.setContext("Test Context");
        mockAdr.setDecision("Test Decision");
        mockAdr.setConsequences("Test Consequences");
        mockAdr.setStatus(ADR.Status.DRAFT);
        mockAdr.setRfc(mockRfc);
        mockAdr.setCreatedAt(Instant.now());
        mockAdr.setUpdatedAt(Instant.now());

        mockRfc.setUserReviewers(new java.util.HashSet<>());
        mockRfc.setContextReviewers(new java.util.HashSet<>());
        mockRfc.setObservers(new java.util.HashSet<>());
    }


    // Create ADR tests
    @Test
    @DisplayName("Should create ADR successfully")
    void shouldCreateAdrSuccessfully() {
        // Given the input request
        CreateAdrRequest request = new CreateAdrRequest(
                "New ADR",
                "Context",
                "Decision",
                "Consequences",
                ADR.Status.DRAFT,
                1L
        );

        when(rfcRepository.findByIdAndOrgId(1L, 1L)).thenReturn(Optional.of(mockRfc));
        when(adrRepository.save(any(ADR.class))).thenReturn(mockAdr);

        // When calling the function to test
        AdrResponse response = adrService.createAdr(1L, 1L, request);

        // Assertions
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test ADR");
        assertThat(response.context()).isEqualTo("Test Context");
        assertThat(response.consequences()).isEqualTo("Test Consequences");
        assertThat(response.decision()).isEqualTo("Test Decision");
        assertThat(response.status()).isEqualTo(ADR.Status.DRAFT);
        assertThat(response.rfcId()).isEqualTo(1L);
        assertThat(response.gitHubUrl()).isNull();

        verify(rfcRepository).findByIdAndOrgId(1L, 1L);
        verify(adrRepository).save(any(ADR.class));
        verify(ingestionService).ingestADR(any(ADR.class));
    }

    @Test
    @DisplayName("Should throw exception when RFC not found during creation")
    void shouldThrowExceptionWhenRfcNotFoundDuringCreation() {
        // Given
        CreateAdrRequest request = new CreateAdrRequest(
                "New ADR",
                "Context",
                "Decision",
                "Consequences",
                ADR.Status.DRAFT,
                999L
        );

        when(rfcRepository.findByIdAndOrgId(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adrService.createAdr(1L, 1L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("RFC not found with id 999");

        verify(rfcRepository).findByIdAndOrgId(999L, 1L);
        verify(adrRepository, never()).save(any(ADR.class));
        verify(ingestionService, never()).ingestADR(any());
    }

    @Test
    @DisplayName("Should trim whitespace when creating ADR")
    void shouldTrimWhitespaceWhenCreatingAdr() {
        // Given
        CreateAdrRequest request = new CreateAdrRequest(
                "  Title with spaces  ",
                "  Context with spaces  ",
                "  Decision with spaces  ",
                "  Consequences with spaces  ",
                ADR.Status.DRAFT,
                1L
        );

        when(rfcRepository.findByIdAndOrgId(1L, 1L)).thenReturn(Optional.of(mockRfc));
        when(adrRepository.save(any(ADR.class))).thenAnswer(invocation -> {
            ADR adr = invocation.getArgument(0);
            assertThat(adr.getTitle()).isEqualTo("Title with spaces");
            assertThat(adr.getContext()).isEqualTo("Context with spaces");
            assertThat(adr.getDecision()).isEqualTo("Decision with spaces");
            assertThat(adr.getConsequences()).isEqualTo("Consequences with spaces");
            return mockAdr;
        });

        // When
        adrService.createAdr(1L, 1L, request);

        // Then
        verify(adrRepository).save(any(ADR.class));
    }


    // Get ADR tests
    // not used so not useful, replaced by the method below
    @Test
    @DisplayName("Should get ADR by ID successfully")
    void shouldGetAdrByIdSuccessfully() {
        // Given
        when(adrRepository.findById(1L)).thenReturn(Optional.of(mockAdr));

        // When
        AdrResponse response = adrService.getAdrById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test ADR");
        assertThat(response.context()).isEqualTo("Test Context");
        assertThat(response.decision()).isEqualTo("Test Decision");
        assertThat(response.consequences()).isEqualTo("Test Consequences");
        assertThat(response.rfcId()).isEqualTo(1L);

        verify(adrRepository).findById(1L);
    }

    // not used so not useful, replaced by the method below
    @Test
    @DisplayName("Should throw exception when ADR not found by ID")
    void shouldThrowExceptionWhenAdrNotFoundById() {
        // Given
        when(adrRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adrService.getAdrById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ADR not found with id 999");

        verify(adrRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get ADR by ID for organization with author flag true")
    void shouldGetAdrByIdForOrgWithAuthorTrue() {
        // Given
        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));

        // When
        AdrSpecificResponse response = adrService.getAdrByIdForOrg(1L, 1L, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.author()).isTrue();
        assertThat(response.title()).isEqualTo("Test ADR");
        assertThat(response.context()).isEqualTo("Test Context");
        assertThat(response.decision()).isEqualTo("Test Decision");
        assertThat(response.consequences()).isEqualTo("Test Consequences");
        assertThat(response.rfcId()).isEqualTo(1L);

        verify(adrRepository).findByIdAndRfc_Org_Id(1L, 1L);
    }

    @Test
    @DisplayName("Should get ADR by ID for organization with author flag false")
    void shouldGetAdrByIdForOrgWithAuthorFalse() {
        // Given
        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));

        // When
        AdrSpecificResponse response = adrService.getAdrByIdForOrg(1L, 1L, 999L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.author()).isFalse();
        assertThat(response.title()).isEqualTo("Test ADR");
        assertThat(response.context()).isEqualTo("Test Context");
        assertThat(response.decision()).isEqualTo("Test Decision");
        assertThat(response.consequences()).isEqualTo("Test Consequences");
        assertThat(response.rfcId()).isEqualTo(1L);

        verify(adrRepository).findByIdAndRfc_Org_Id(1L, 1L);
    }

    @Test
    @DisplayName("Should throw exception when ADR not found by ID")
    void shouldThrowExceptionWhenAdrNotFoundByIdForOrg() {
        // Given
        when(adrRepository.findByIdAndRfc_Org_Id(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adrService.getAdrByIdForOrg(999L, 1L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ADR not found with id 999");

        verify(adrRepository).findByIdAndRfc_Org_Id(999L, 1L);
    }


    // List ADRs tests
    // not used so not useful
    @Test
    @DisplayName("Should list all ADRs with pagination")
    void shouldListAllAdrsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ADR> adrPage = new PageImpl<>(List.of(mockAdr));

        when(adrRepository.findAll(pageable)).thenReturn(adrPage);

        // When
        Page<AdrResponse> response = adrService.listAdrs(pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).id()).isEqualTo(1L);

        verify(adrRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should list ADRs by organization with pagination")
    void shouldListAdrsByOrgWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ADR> adrPage = new PageImpl<>(List.of(mockAdr));

        when(adrRepository.findByRfc_Org_Id(1L, pageable)).thenReturn(adrPage);

        // When
        Page<AdrResponse> response = adrService.listAdrsByOrg(1L, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).rfcId()).isEqualTo(1L);

        verify(adrRepository).findByRfc_Org_Id(1L, pageable);
    }


    // Create ADR draft tests
    // not used it seems, so not useful
    @Test
    @DisplayName("Should create draft from RFC and Alternative")
    void shouldCreateDraftFromRfcAndAlternative() {
        // Given
        ADR expectedAdr = new ADR();
        expectedAdr.setId(1L);
        expectedAdr.setRfc(mockRfc);
        expectedAdr.setStatus(ADR.Status.DRAFT);
        expectedAdr.setTitle(mockRfc.getTitle());
        expectedAdr.setContext(mockRfc.getDescription());
        expectedAdr.setDecision(mockAlternative.getTitle());
        expectedAdr.setConsequences("");

        when(adrRepository.save(any(ADR.class))).thenReturn(expectedAdr);

        // When
        ADR result = adrService.createDraftFromRfcAndAlternative(mockRfc, mockAlternative);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ADR.Status.DRAFT);
        assertThat(result.getTitle()).contains("Test RFC");
        assertThat(result.getContext()).contains("Test RFC Description");
        assertThat(result.getDecision()).contains("Strangler Pattern");

        verify(adrRepository).save(any(ADR.class));
    }

    // not used it seems, so not useful
    @Test
    @DisplayName("Should create draft from RFC without Alternative")
    void shouldCreateDraftFromRfcWithoutAlternative() {
        // Given
        ADR expectedAdr = new ADR();
        expectedAdr.setId(1L);
        expectedAdr.setRfc(mockRfc);
        expectedAdr.setStatus(ADR.Status.DRAFT);

        when(adrRepository.save(any(ADR.class))).thenReturn(expectedAdr);

        // When
        ADR result = adrService.createDraftFromRfcAndAlternative(mockRfc, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ADR.Status.DRAFT);

        verify(adrRepository).save(any(ADR.class));
    }


    // Update ADRs tests
    @Test
    @DisplayName("Should update ADR for organization successfully")
    void shouldUpdateAdrForOrgSuccessfully() {
        // Given
        UpdateAdrRequest request = new UpdateAdrRequest(
                "Updated Title",
                "Updated Context",
                "Updated Decision",
                "Updated Consequences",
                ADR.Status.APPROVED
        );

        ADR updatedAdr = new ADR();
        updatedAdr.setId(1L);
        updatedAdr.setTitle("Updated Title");
        updatedAdr.setContext("Updated Context");
        updatedAdr.setDecision("Updated Decision");
        updatedAdr.setConsequences("Updated Consequences");
        updatedAdr.setStatus(ADR.Status.APPROVED);
        updatedAdr.setRfc(mockRfc);
        updatedAdr.setCreatedAt(Instant.now());
        updatedAdr.setUpdatedAt(Instant.now());

        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));
        when(adrRepository.save(any(ADR.class))).thenReturn(updatedAdr);

        // When
        AdrResponse response = adrService.updateAdrForOrg(1L, 1L, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(response.context()).isEqualTo("Updated Context");
        assertThat(response.decision()).isEqualTo("Updated Decision");
        assertThat(response.consequences()).isEqualTo("Updated Consequences");
        assertThat(response.status()).isEqualTo(ADR.Status.APPROVED);
        assertThat(response.rfcId()).isEqualTo(1L);

        verify(adrRepository).findByIdAndRfc_Org_Id(1L, 1L);
        verify(adrRepository).save(any(ADR.class));
        verify(ingestionService).ingestADR(any(ADR.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent ADR")
    void shouldThrowExceptionWhenUpdatingNonExistentAdr() {
        // Given
        UpdateAdrRequest request = new UpdateAdrRequest(
                "Updated Title",
                "Updated Context",
                "Updated Decision",
                "Updated Consequences",
                ADR.Status.APPROVED
        );

        when(adrRepository.findByIdAndRfc_Org_Id(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adrService.updateAdrForOrg(999L, 1L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ADR not found with id 999");

        verify(adrRepository).findByIdAndRfc_Org_Id(999L, 1L);
        verify(adrRepository, never()).save(any(ADR.class));
        verify(ingestionService, never()).ingestADR(any());
    }

    @Test
    @DisplayName("Should handle null values in update request")
    void shouldHandleNullValuesInUpdateRequest() {
        // Given
        UpdateAdrRequest request = new UpdateAdrRequest(
                null,
                null,
                null,
                null,
                ADR.Status.APPROVED
        );

        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));
        when(adrRepository.save(any(ADR.class))).thenReturn(mockAdr);

        // When
        AdrResponse response = adrService.updateAdrForOrg(1L, 1L, request);

        // Then
        assertThat(response).isNotNull();
        verify(adrRepository).save(any(ADR.class));
    }


    // Publish ADRs tests
    @Test
    @DisplayName("Should publish ADR to GitHub and trigger notifications successfully")
    void shouldPublishAdrToGitHubSuccessfully() throws Exception {
        // Given
        PublishAdrRequest request = new PublishAdrRequest(1L);
        mockAdr.setGitHubUrl(null);

        // Setup Mock behavior
        when(adrRepository.findById(1L)).thenReturn(Optional.of(mockAdr));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Mock GitHub API response
        String githubResponseJson = "{\"content\": {\"html_url\": \"https://github.com/repo/adr-1.md\"}}";
        ResponseEntity<String> githubResponse = new ResponseEntity<>(githubResponseJson, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(githubResponse);

        // Mock ObjectMapper per restituire un nodo reale (meno fragile dei mock di JsonNode)
        ObjectMapper realMapper = new ObjectMapper();
        JsonNode actualNode = realMapper.readTree(githubResponseJson);
        when(objectMapper.readTree(githubResponseJson)).thenReturn(actualNode);

        when(adrRepository.save(any(ADR.class))).thenReturn(mockAdr);

        // When
        AdrResponse response = adrService.publishAdr(request, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.gitHubUrl()).isEqualTo("https://github.com/repo/adr-1.md");

        verify(eventPublisher).publishEvent(any(dsd.api.cdmsa.model.event.AdrPublishedEvent.class));

        verify(adrRepository).findById(1L);
        verify(ingestionService).ingestADR(any(ADR.class));
    }

    @Test
    @DisplayName("Should throw exception when ADR not found during publish")
    void shouldThrowExceptionWhenAdrNotFoundDuringPublish() {
        // Given
        PublishAdrRequest request = new PublishAdrRequest(999L);

        when(adrRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adrService.publishAdr(request, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ADR not found with id 999");

        verify(adrRepository).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(restTemplate, never()).exchange(
                anyString(),
                any(HttpMethod.class),
                any(),
                eq(String.class)
        );
    }

    @Test
    @DisplayName("Should throw exception when user not found during publish")
    void shouldThrowExceptionWhenUserNotFoundDuringPublish() {
        // Given
        PublishAdrRequest request = new PublishAdrRequest(1L);

        when(adrRepository.findById(1L)).thenReturn(Optional.of(mockAdr));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adrService.publishAdr(request, 999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(adrRepository).findById(1L);
        verify(userRepository).findById(999L);
        verify(restTemplate, never()).exchange(
                anyString(),
                any(HttpMethod.class),
                any(),
                eq(String.class)
        );
    }

    @Test
    @DisplayName("Should throw RuntimeException when GitHub API fails")
    void shouldThrowRuntimeExceptionWhenGitHubApiFails() {
        // Given
        PublishAdrRequest request = new PublishAdrRequest(1L);

        when(adrRepository.findById(1L)).thenReturn(Optional.of(mockAdr));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Mock GitHub API error response
        ResponseEntity<String> errorResponse = new ResponseEntity<>(
                "{\"message\":\"Bad credentials\"}",
                HttpStatus.UNAUTHORIZED
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(errorResponse);

        // When & Then
        assertThatThrownBy(() -> adrService.publishAdr(request, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to push ADR to GitHub");

        verify(adrRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(adrRepository, never()).save(any(ADR.class));
    }

    @Test
    @DisplayName("Should delete ADR successfully")
    void shouldDeleteAdrSuccessfully() {
        // Given
        // The user is the author (1L == 1L)
        // ADR is DRAFT and not published on GitHub
        mockAdr.setStatus(ADR.Status.DRAFT);
        mockAdr.setGitHubUrl(null);

        // Mocks for finding the ADR and the alternatives associated with the RFC
        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));
        when(alternativeRepository.findByRfcId(mockRfc.getId())).thenReturn(List.of(mockAlternative));
        when(rfcRepository.save(any(RFC.class))).thenReturn(mockRfc);

        // When
        adrService.deleteAdr(1L, 1L, 1L);

        // Then
        // 1. Verify Winning Alternative was reset to false
        verify(alternativeRepository).findByRfcId(mockRfc.getId());
        verify(alternativeRepository).save(argThat(alt ->
                alt.getId().equals(mockAlternative.getId()) && !alt.getIsWinning()
        ));

        // 2. Verify RFC Status Reset
        verify(rfcRepository).save(argThat(rfc ->
                rfc.getStatus() == RFC.Status.UNDER_REVIEW && rfc.getAdr() == null
        ));
        verify(ingestionService).ingestRFC(any(RFC.class));

        // 3. Verify ADR Deletion
        verify(adrRepository).delete(mockAdr);
        verify(ingestionService).deleteADR(mockAdr.getId());
    }

    @Test
    @DisplayName("Should throw exception when deleting ADR if User is not Author")
    void shouldThrowExceptionWhenDeletingAdrIfNotAuthor() {
        // Given
        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));

        // When: Calling with userId 999L (not the author)
        assertThatThrownBy(() -> adrService.deleteAdr(1L, 1L, 999L))
                .isInstanceOf(RfcAlternativeNotAllowedException.class)
                .hasMessageContaining("Only the author can delete this ADR");

        // Then: verify no deletions occurred
        verify(adrRepository, never()).delete(any());
        verify(ingestionService, never()).deleteADR(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when deleting ADR if Status is APPROVED")
    void shouldThrowExceptionWhenDeletingAdrIfApproved() {
        // Given
        mockAdr.setStatus(ADR.Status.APPROVED);
        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));

        // When & Then
        assertThatThrownBy(() -> adrService.deleteAdr(1L, 1L, 1L))
                .isInstanceOf(RfcInvalidStatusException.class)
                .hasMessageContaining("Cannot delete an ADR that has already been published/approved");

        verify(adrRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting ADR if GitHub URL is present")
    void shouldThrowExceptionWhenDeletingAdrIfPublished() {
        // Given
        mockAdr.setStatus(ADR.Status.DRAFT);
        mockAdr.setGitHubUrl("https://github.com/something");
        when(adrRepository.findByIdAndRfc_Org_Id(1L, 1L)).thenReturn(Optional.of(mockAdr));

        // When & Then
        assertThatThrownBy(() -> adrService.deleteAdr(1L, 1L, 1L))
                .isInstanceOf(RfcInvalidStatusException.class)
                .hasMessageContaining("Cannot delete an ADR that has already been published/approved");

        verify(adrRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent ADR")
    void shouldThrowExceptionWhenDeletingNonExistentAdr() {
        // Given
        when(adrRepository.findByIdAndRfc_Org_Id(999L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adrService.deleteAdr(999L, 1L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== UTILITY TESTS ====================

    @Test
    @DisplayName("Should build markdown correctly")
    void shouldBuildMarkdownCorrectly() {
        // When
        String markdown = adrService.buildMarkdown(mockAdr);

        // Then
        assertThat(markdown).isNotNull();
        assertThat(markdown).contains("# Test ADR");
        assertThat(markdown).contains("## Context");
        assertThat(markdown).contains("Test Context");
        assertThat(markdown).contains("## Decision");
        assertThat(markdown).contains("Test Decision");
        assertThat(markdown).contains("## Consequences");
        assertThat(markdown).contains("Test Consequences");
        assertThat(markdown).contains("## Status");
        assertThat(markdown).contains("DRAFT");
    }
}