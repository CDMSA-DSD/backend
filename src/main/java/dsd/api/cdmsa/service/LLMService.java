package dsd.api.cdmsa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dsd.api.cdmsa.dto.GenerateAdrRequest;
import dsd.api.cdmsa.dto.GenerateAdrResponse;
import dsd.api.cdmsa.exception.RfcNotFoundException;
import dsd.api.cdmsa.model.Alternative;
import dsd.api.cdmsa.model.RFC;
import dsd.api.cdmsa.repository.AlternativeRepository;
import dsd.api.cdmsa.repository.RfcRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@Service
public class LLMService {

    private final RfcRepository rfcRepository;
    private final AlternativeRepository alternativeRepository;
    private final ChatClient llm;
    private final ObjectMapper objectMapper;

    public LLMService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, RfcRepository rfcRepository, AlternativeRepository alternativeRepository) {
        this.llm = chatClientBuilder.build();
        this.rfcRepository = rfcRepository;
        this.alternativeRepository = alternativeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GenerateAdrResponse createDraftFromRfcAndAlternative(Long rfcId, Long userId, GenerateAdrRequest request) {

        RFC rfc = rfcRepository.findById(rfcId)
                .orElseThrow(() -> new RfcNotFoundException("RFC not found"));

        Alternative alternative = alternativeRepository.findById(request.alternativeId())
                .orElseThrow(() -> new RfcNotFoundException("Alternative not found"));

        // Create prompt for LLM
        String prompt = buildPrompt(rfc, alternative);

        // Call LLM API thanks to spring AI
        LlmAdrContent content;
        try {
            content = llm.prompt()
                    .user(prompt)
                    .call()
                    .entity(LlmAdrContent.class);
        } catch (Exception e) {
            log.error("Error in automatic parsing", e);
            // Fallback: try manual parsing
            String generatedContent = llm.prompt()
                    .user(prompt)
                    .call()
                    .content();
            content = parseManually(generatedContent);
        }


        return new GenerateAdrResponse(
                "ADR for RFC #" + rfc.getId() + ": " + rfc.getTitle(),
                content.context(),
                content.decision(),
                content.consequences()
        );
    }

    private String buildPrompt(RFC rfc, Alternative alternative) {
        String commentsSection = "";
        if (rfc.getComments() != null && !rfc.getComments().isEmpty()) {
            StringBuilder comments = new StringBuilder();
            comments.append("Comments:\n");
            rfc.getComments().forEach(comment ->
                    comments.append("- ").append(comment.getContent()).append("\n")
            );
            commentsSection = comments.toString();
        } else {
            commentsSection = "No comment available";
        }

        return String.format("""
        Generate a complete Architecture Decision Record (ADR) in JSON format.
        
        RFC DATA:
        Title: %s
        Description: %s
        
        %s
        
        SELECTED ALTERNATIVE:
        Name: %s
        Description: %s
        Pros: %s
        Cons: %s
        
        INSTRUCTIONS:
        Create a professional ADR based on ALL provided data, including community comments.
        Comments may contain important considerations, risks, or suggestions to include in the analysis.
        
        Respond ONLY with a pure JSON object (no markdown, no code blocks):
        {"context":"...","decision":"...","consequences":"..."}
        
        - 'context': describe the problem, requirements, and context that led to this decision
        - 'decision': explain the decision made, the chosen alternative, and the main reasons
        - 'consequences': list positive and negative consequences, considering pros/cons and feedback from comments
        """,
                rfc.getTitle(),
                rfc.getDescription() != null ? rfc.getDescription() : "N/A",
                commentsSection,
                alternative != null ? alternative.getTitle() : "N/A",
                alternative != null && alternative.getDescription() != null ? alternative.getDescription() : "",
                alternative != null && alternative.getPros() != null ? alternative.getPros() : "",
                alternative != null && alternative.getCons() != null ? alternative.getCons() : ""
        );
    }

    private LlmAdrContent parseManually(String response) {
        try {
            String json = response.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(json, LlmAdrContent.class);
        } catch (Exception e) {
            log.error("Error in manuale parsing", e);
            return new LlmAdrContent(
                    response,
                    "Manual review needed",
                    ""
            );
        }
    }

    // LLM answer
    private record LlmAdrContent(String context, String decision, String consequences) {}
}
