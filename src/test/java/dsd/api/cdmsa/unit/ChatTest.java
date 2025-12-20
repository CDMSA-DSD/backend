package dsd.api.cdmsa.unit;

import dsd.api.cdmsa.exception.ChatBotException;
import dsd.api.cdmsa.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private VectorStore vectorStore;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        // Mocking the builder chain used in constructor
        when(chatClientBuilder.defaultOptions(any(ChatOptions.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Instantiate service
        chatService = new ChatService(chatClientBuilder, vectorStore);

        // Inject @Value fields manually since we are not using Spring Context
        ReflectionTestUtils.setField(chatService, "similarityThreshold", 0.7);
        ReflectionTestUtils.setField(chatService, "topK", 3);
        ReflectionTestUtils.setField(chatService, "temperature", 0.3);
    }

    @Test
    @DisplayName("Should return warning if input is empty")
    void shouldReturnWarningIfInputIsEmpty() {
        String response = chatService.generateResponse("", 1L);
        assertThat(response).isEqualTo("Please, formulate a valid question");

        String responseNull = chatService.generateResponse(null, 1L);
        assertThat(responseNull).isEqualTo("Please, formulate a valid question");

        verifyNoInteractions(vectorStore);
        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("Should return no pertinent documents found if vector store returns empty")
    void shouldReturnNoDocsFoundIfVectorStoreEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        String response = chatService.generateResponse("How to deploy?", 1L);

        assertThat(response).isEqualTo("No pertinents ADRs or RFCs found to answer the question");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("Should perform RAG flow successfully")
    void shouldPerformRagFlowSuccessfully() {
        // Given
        String query = "What is the database?";
        Long orgId = 100L;

        Document doc1 = new Document("Content of Doc 1", Map.of("title", "DB Selection", "type", "ADR", "source_id", "5"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1));

        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Use PostgreSQL <s>");

        // When
        String response = chatService.generateResponse(query, orgId);

        // Then
        assertThat(response).isEqualTo("Use PostgreSQL");

        // Verify Vector Store Search Params
        ArgumentCaptor<SearchRequest> searchCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(searchCaptor.capture());

        SearchRequest capturedRequest = searchCaptor.getValue();
        assertThat(capturedRequest.getQuery()).isEqualTo("what is the database?");
        assertThat(capturedRequest.getTopK()).isEqualTo(3);
        assertThat(capturedRequest.getSimilarityThreshold()).isEqualTo(0.7);

        String filterString = capturedRequest.getFilterExpression().toString();
        assertThat(filterString).contains("org_id");
        assertThat(filterString).contains("100");

        // Verify Prompt Context
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatClient).prompt(promptCaptor.capture());

        String promptText = promptCaptor.getValue().getContents();
        assertThat(promptText).contains("Content of Doc 1");
        assertThat(promptText).contains("DB Selection");
        assertThat(promptText).contains("What is the database?");
    }

    @Test
    @DisplayName("Should handle exceptions and throw ChatBotException")
    void shouldHandleExceptionsAndThrowChatBotException() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("Vector DB Down"));

        assertThatThrownBy(() -> chatService.generateResponse("query", 1L))
                .isInstanceOf(ChatBotException.class)
                .hasMessageContaining("AI Processing failed");
    }

    @Test
    @DisplayName("Should preprocess query correctly")
    void shouldPreprocessQueryCorrectly() {
        // Given input with special chars and extra spaces
        String dirtyQuery = "  Hello!!!   World...  ";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        chatService.generateResponse(dirtyQuery, 1L);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        String processed = captor.getValue().getQuery();
        // Regex in service keeps [a-zA-Z0-9\s.,?-]
        assertThat(processed).isEqualTo("hello world...");
    }
}