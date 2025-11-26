package dsd.api.cdmsa.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import dsd.api.cdmsa.dto.GenerateAdrRequest;
import dsd.api.cdmsa.dto.GenerateAdrResponse;
import dsd.api.cdmsa.exception.RfcNotFoundException;
import dsd.api.cdmsa.model.*;
import dsd.api.cdmsa.repository.AlternativeRepository;
import dsd.api.cdmsa.repository.RfcRepository;
import dsd.api.cdmsa.service.LLMService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LLMService Complete Test Suite")
class LLMServiceTest {

    @Mock private RfcRepository rfcRepository;
    @Mock private AlternativeRepository alternativeRepository;
    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;

    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec responseSpec;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LLMService llmService;
    private RFC mockRfc;
    private Alternative mockAlternative;

    @BeforeEach
    void setUp() {
        // Setting up RFC with basic fields
        mockRfc = new RFC();
        mockRfc.setId(1L);
        mockRfc.setTitle("Microservices Migration");
        mockRfc.setDescription("Migrate monolithic app");
        mockRfc.setComments(Set.of());

        // Setting up Alternative with basic fields
        mockAlternative = new Alternative();
        mockAlternative.setId(2L);
        mockAlternative.setTitle("Strangler Pattern");
        mockAlternative.setPros("Low risk");

        when(chatClientBuilder.build()).thenReturn(chatClient);

        llmService = new LLMService(chatClientBuilder, objectMapper, rfcRepository, alternativeRepository);
    }


    // Generation of ADR tests
    @Test
    @DisplayName("Happy Path: Automatic parsing works successfully")
    void shouldGenerateAdrSuccessfully() {
        GenerateAdrRequest request = new GenerateAdrRequest(2L);
        LLMService.LlmAdrContent expectedContent = new LLMService.LlmAdrContent("Ctx", "Dec", "Cons");

        when(rfcRepository.findById(1L)).thenReturn(Optional.of(mockRfc));
        when(alternativeRepository.findById(2L)).thenReturn(Optional.of(mockAlternative));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.entity(eq(LLMService.LlmAdrContent.class))).thenReturn(expectedContent);

        GenerateAdrResponse response = llmService.createDraftFromRfcAndAlternative(1L, 1L, request);

        assertThat(response.context()).isEqualTo("Ctx");
        assertThat(response.decision()).isEqualTo("Dec");
        assertThat(response.consequences()).isEqualTo("Cons");
        verify(responseSpec, never()).content();
    }

    @Test
    @DisplayName("Verification: Should build prompt with correct RFC details")
    void shouldBuildPromptCorrectly() {
        GenerateAdrRequest request = new GenerateAdrRequest(2L);
        LLMService.LlmAdrContent dummyContent = new LLMService.LlmAdrContent("C", "D", "Q");

        when(rfcRepository.findById(1L)).thenReturn(Optional.of(mockRfc));
        when(alternativeRepository.findById(2L)).thenReturn(Optional.of(mockAlternative));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.entity(eq(LLMService.LlmAdrContent.class))).thenReturn(dummyContent);

        // Captor
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(requestSpec.user(promptCaptor.capture())).thenReturn(requestSpec);

        llmService.createDraftFromRfcAndAlternative(1L, 1L, request);

        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Microservices Migration");
        assertThat(capturedPrompt).contains("Strangler Pattern");
        assertThat(capturedPrompt).contains("JSON object");
    }


    // Fallbacks and parsing tests
    @Test
    @DisplayName("Fallback: Should trigger fallback logic AND clean markdown code blocks")
    void shouldFallbackAndCleanMarkdown() {
        GenerateAdrRequest request = new GenerateAdrRequest(2L);

        // Raw string
        String rawJsonString = """
            ```json
            {
                "context": "Manual Context",
                "decision": "Manual Decision",
                "consequences": "Manual Consequences"
            }
            ```
            """;

        when(rfcRepository.findById(1L)).thenReturn(Optional.of(mockRfc));
        when(alternativeRepository.findById(2L)).thenReturn(Optional.of(mockAlternative));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        // entity() fails
        when(responseSpec.entity(eq(LLMService.LlmAdrContent.class)))
                .thenThrow(new RuntimeException("AI Mapping Error"));

        // content() returns the raw string
        when(responseSpec.content()).thenReturn(rawJsonString);

        GenerateAdrResponse response = llmService.createDraftFromRfcAndAlternative(1L, 1L, request);

        assertThat(response.context()).isEqualTo("Manual Context");
        verify(responseSpec).content();
    }

    @Test
    @DisplayName("Validation: Should trigger fallback if entity returns valid object but empty fields")
    void shouldTriggerFallbackWhenValidationFails() {
        GenerateAdrRequest request = new GenerateAdrRequest(2L);

        // Valid object but empty
        LLMService.LlmAdrContent invalidContent = new LLMService.LlmAdrContent("", null, "  ");
        String fallbackJson = "{\"context\":\"Fixed\",\"decision\":\"Fixed\",\"consequences\":\"Fixed\"}";

        when(rfcRepository.findById(1L)).thenReturn(Optional.of(mockRfc));
        when(alternativeRepository.findById(2L)).thenReturn(Optional.of(mockAlternative));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        // Object empty -> Illegal state -> catch
        when(responseSpec.entity(eq(LLMService.LlmAdrContent.class))).thenReturn(invalidContent);
        when(responseSpec.content()).thenReturn(fallbackJson);

        GenerateAdrResponse response = llmService.createDraftFromRfcAndAlternative(1L, 1L, request);

        assertThat(response.context()).isEqualTo("Fixed");
    }

    @Test
    @DisplayName("Last Resort: Should return Error Object when even manual parsing fails")
    void shouldReturnErrorObjectWhenManualParsingFails() {
        GenerateAdrRequest request = new GenerateAdrRequest(2L);
        String garbageResponse = "Not JSON";

        when(rfcRepository.findById(1L)).thenReturn(Optional.of(mockRfc));
        when(alternativeRepository.findById(2L)).thenReturn(Optional.of(mockAlternative));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        when(responseSpec.entity(eq(LLMService.LlmAdrContent.class))).thenThrow(new RuntimeException("Fail 1"));
        when(responseSpec.content()).thenReturn(garbageResponse);

        GenerateAdrResponse response = llmService.createDraftFromRfcAndAlternative(1L, 1L, request);

        assertThat(response.context()).contains("Error:");
        assertThat(response.decision()).contains("Manual review required");
    }


    // Testing exceptions
    @Test
    @DisplayName("Error: Should throw RfcNotFoundException if RFC missing")
    void shouldThrowIfRfcNotFound() {
        GenerateAdrRequest request = new GenerateAdrRequest(2L);
        when(rfcRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> llmService.createDraftFromRfcAndAlternative(99L, 1L, request))
                .isInstanceOf(RfcNotFoundException.class)
                .hasMessageContaining("RFC not found");

        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("Error: Should throw RfcNotFoundException if Alternative missing")
    void shouldThrowIfAlternativeNotFound() {
        GenerateAdrRequest request = new GenerateAdrRequest(99L);
        when(rfcRepository.findById(1L)).thenReturn(Optional.of(mockRfc));
        when(alternativeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> llmService.createDraftFromRfcAndAlternative(1L, 1L, request))
                .isInstanceOf(RfcNotFoundException.class)
                .hasMessageContaining("Alternative not found");

        verifyNoInteractions(chatClient);
    }
}