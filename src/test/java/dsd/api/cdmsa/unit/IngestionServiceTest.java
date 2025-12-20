package dsd.api.cdmsa.unit;

import dsd.api.cdmsa.model.*;
import dsd.api.cdmsa.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IngestionService Tests")
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private IngestionService ingestionService;

    @Captor
    private ArgumentCaptor<List<Document>> documentsCaptor;

    private RFC mockRfc;
    private ADR mockAdr;
    private User mockUser;
    private Organization mockOrg;

    @BeforeEach
    void setUp() {
        mockOrg = new Organization();
        mockOrg.setId(99L);
        mockOrg.setName("Test Corp");

        mockUser = new User();
        mockUser.setId(10L);
        mockUser.setFirstname("Mario");
        mockUser.setLastname("Rossi");

        mockRfc = new RFC();
        mockRfc.setId(1L);
        mockRfc.setTitle("Refactor Monolith");
        mockRfc.setDescription("We need to split the backend.");
        mockRfc.setStatus(RFC.Status.UNDER_REVIEW);
        mockRfc.setCreatedAt(Instant.parse("2023-01-01T10:00:00Z"));
        mockRfc.setOrg(mockOrg);
        mockRfc.setUser(mockUser);

        mockAdr = new ADR();
        mockAdr.setId(2L);
        mockAdr.setTitle("Use Microservices");
        mockAdr.setStatus(ADR.Status.APPROVED);
        mockAdr.setContext("The monolith is too big.");
        mockAdr.setDecision("We will use Spring Boot microservices.");
        mockAdr.setConsequences("More complexity in deployment.");
        mockAdr.setCreatedAt(Instant.parse("2023-02-01T10:00:00Z"));
        mockAdr.setRfc(mockRfc);
    }

    @Test
    @DisplayName("Should ingest RFC successfully checking Content and Metadata")
    void shouldIngestRfcSuccessfully() {
        Alternative alt1 = new Alternative();
        alt1.setTitle("Lambda Functions");
        alt1.setDescription("Serverless approach");
        alt1.setPros("Cheap");
        alt1.setCons("Cold starts");
        alt1.setIsWinning(false);

        Alternative alt2 = new Alternative();
        alt2.setTitle("K8s Containers");
        alt2.setDescription("Containerized approach");
        alt2.setIsWinning(true);

        Comment comment = new Comment();
        comment.setContent("I prefer K8s");
        comment.setAuthor(mockUser);

        mockRfc.setAlternatives(Set.of(alt1, alt2));
        mockRfc.setComments(Set.of(comment));
        mockRfc.setStatus(RFC.Status.UNDER_REVIEW);

        ingestionService.ingestRFC(mockRfc);

        verify(vectorStore).add(documentsCaptor.capture());

        List<Document> capturedDocs = documentsCaptor.getValue();
        assertThat(capturedDocs).hasSize(1);
        Document doc = capturedDocs.get(0);

        String expectedId = UUID.nameUUIDFromBytes(("RFC_" + mockRfc.getId()).getBytes()).toString();
        assertThat(doc.getId()).isEqualTo(expectedId);

        String content = doc.getText();
        assertThat(content).contains("=== RFC (Request for Comments) ===");
        assertThat(content).contains("TITLE: Refactor Monolith");
        assertThat(content).contains("AUTHOR: Mario Rossi");
        assertThat(content).contains("DESCRIPTION:\nWe need to split the backend.");
        assertThat(content).contains(">>> ALTERNATIVE: Lambda Functions");
        assertThat(content).contains(">>> ALTERNATIVE: K8s Containers [SELECTED DECISION]");
        assertThat(content).contains("- Mario Rossi wrote: I prefer K8s");

        Map<String, Object> metadata = doc.getMetadata();
        assertThat(metadata).containsEntry("type", "RFC");
        assertThat(metadata).containsEntry("source_id", "1");
        assertThat(metadata).containsEntry("org_id", "99");
        assertThat(metadata).containsEntry("status", RFC.Status.UNDER_REVIEW);
    }

    @Test
    @DisplayName("Should throw RuntimeException when RFC ingestion fails")
    void shouldThrowRuntimeExceptionWhenRfcIngestionFails() {
        doThrow(new RuntimeException("DB Connection Error")).when(vectorStore).add(anyList());

        assertThatThrownBy(() -> ingestionService.ingestRFC(mockRfc))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to ingest RFC");
    }

    @Test
    @DisplayName("Should ingest ADR successfully")
    void shouldIngestAdrSuccessfully() {
        ingestionService.ingestADR(mockAdr);

        verify(vectorStore).add(documentsCaptor.capture());
        Document doc = documentsCaptor.getValue().get(0);

        String expectedId = UUID.nameUUIDFromBytes(("ADR_" + mockAdr.getId()).getBytes()).toString();
        assertThat(doc.getId()).isEqualTo(expectedId);

        String content = doc.getText();
        assertThat(content).contains("=== ADR (Architectural Decision Record) ===");
        assertThat(content).contains("TITLE: Use Microservices");
        assertThat(content).contains("CONTEXT:\nThe monolith is too big.");

        Map<String, Object> metadata = doc.getMetadata();
        assertThat(metadata).containsEntry("type", "ADR");
        assertThat(metadata).containsEntry("source_id", "2");
        assertThat(metadata).containsEntry("org_id", "99");
        assertThat(metadata).containsEntry("status", ADR.Status.APPROVED);
    }

    @Test
    @DisplayName("Should throw RuntimeException when ADR ingestion fails")
    void shouldThrowRuntimeExceptionWhenAdrIngestionFails() {
        doThrow(new RuntimeException("API Error")).when(vectorStore).add(anyList());

        assertThatThrownBy(() -> ingestionService.ingestADR(mockAdr))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to ingest ADR");
    }

    @Test
    @DisplayName("Should delete ADR successfully")
    void shouldDeleteAdrSuccessfully() {
        ingestionService.deleteADR(55L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> idListCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).delete(idListCaptor.capture());

        List<String> deletedIds = idListCaptor.getValue();
        assertThat(deletedIds).hasSize(1);
        String expectedId = UUID.nameUUIDFromBytes("ADR_55".getBytes()).toString();
        assertThat(deletedIds.get(0)).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("Should log error but NOT throw exception when delete fails")
    void shouldLogButNotThrowWhenDeleteFails() {
        doThrow(new RuntimeException("Delete failed")).when(vectorStore).delete(anyList());

        ingestionService.deleteADR(55L);

        verify(vectorStore).delete(anyList());
    }
}