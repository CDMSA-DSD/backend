package dsd.api.cdmsa.service;

import dsd.api.cdmsa.exception.ChatBotException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // Parameters that we can change and play with
    @Value("${chatbot.similarity.threshold:0.5}")
    private double similarityThreshold;

    @Value("${chatbot.similarity.top-k:4}")
    private int topK;

    // Temperature says how creative the AI can be
    @Value("${chatbot.llm.temperature:0.3}")
    private double temperature;

    private final String promptTemplate = """
        You are an AI assistant specialized in the CDMSA system (Change Decision Management for Software Architecture).
        
        ROLE: Provide precise technical answers based exclusively on available documentation (RFCs and ADRs).
        
        INSTRUCTIONS:
        1. Carefully analyze the CONTEXT provided below (RFCs and ADRs)
        2. Answer ONLY if the context contains relevant information
        3. ALWAYS cite your sources (e.g., "According to RFC 'Database Migration'..." or "As stated in ADR 'Pattern Selection'...")
        4. If the context is insufficient, respond exactly: "I don't have enough information in the CDMSA documentation to answer this question. I suggest consulting the architecture team directly."
        5. Use technical but clear language
        6. If related but not directly relevant documents are found, mention them briefly
        7. Keep your answer concise and focused
        
        EXAMPLE INTERACTIONS:
        
        Q: "What database did we choose for the project?"
        Context: [ADR: Database Selection] "We decided to use PostgreSQL for its ACID compliance and JSON support..."
        A: "According to ADR 'Database Selection', the team chose PostgreSQL primarily for its ACID compliance and native JSON support."
        
        Q: "How do we handle authentication?"
        Context: [RFC: API Security] "Proposed JWT-based authentication with refresh tokens..."
        A: "Based on RFC 'API Security', the proposed approach is JWT-based authentication with refresh tokens."
        
        Q: "What's our deployment strategy?"
        Context: [Unrelated document about frontend architecture]
        A: "I don't have enough information in the CDMSA documentation to answer this question about deployment strategy."
        
        ---
        
        DOCUMENTATION CONTEXT:
        {context}
        
        USER QUESTION:
        {question}
        
        YOUR ANSWER:
        """;

    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
                .defaultOptions(ChatOptions.builder()
                        .temperature(temperature)
                        .build())
                .build();
        this.vectorStore = vectorStore;
    }

    public String generateResponse(String message, Long orgId) {

        if (message == null || message.trim().isEmpty()) {
            return "Please, formulate a valid question";
        }

        String processedQuery = preprocessQuery(message);

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(processedQuery)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression("org_id == '" + orgId.toString() + "'")
                    .build();

            List<Document> similarDocuments = vectorStore.similaritySearch(request);

            log.info("RAG Search: found {} documents for query '{}'", similarDocuments.size(), message);

            if (similarDocuments.isEmpty()) {
                return "No pertinents ADRs or RFCs found to answer the question";
            }

            String context = formatContext(similarDocuments);

            PromptTemplate template = new PromptTemplate(promptTemplate);
            Prompt prompt = template.create(Map.of(
                    "context", context,
                    "question", message
            ));

            String resp = chatClient.prompt(prompt)
                    .call()
                    .content();

            return resp.replace("<s>", "").trim();

        } catch (Exception e) {
            log.error("Error generating AI response", e);
            throw new ChatBotException("AI Processing failed: " + e.getMessage(), e);
        }
    }

    private String preprocessQuery(String query) {
        if (query == null) return "";
        String processed = query.trim().toLowerCase();

        processed = processed.replaceAll("[^a-zA-Z0-9\\s\\.,\\?\\-]", " ");

        processed = processed.replaceAll("\\s+", " ");

        return processed;
    }

    private String formatContext(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    String type = (String) doc.getMetadata().getOrDefault("type", "DOC");
                    String title = (String) doc.getMetadata().getOrDefault("title", "Senza titolo");
                    String sourceId = (String) doc.getMetadata().getOrDefault("source_id", "N/A");

                    return String.format("""
                            [%s: %s (ID: %s)]
                            %s
                            """, type, title, sourceId, doc.getText());
                })
                .collect(Collectors.joining("\n---\n"));
    }
}