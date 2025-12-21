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
            
            // Validate that all required fields are present and not empty
            if (content.context() == null || content.context().trim().isEmpty() ||
                content.decision() == null || content.decision().trim().isEmpty() ||
                content.consequences() == null || content.consequences().trim().isEmpty()) {
                throw new IllegalStateException("LLM response missing required fields");
            }
        } catch (Exception e) {
            log.error("Error in automatic parsing, attempting manual parse", e);
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
            commentsSection = "No comments available";
        }

        return String.format("""
        You are an expert technical writer creating Architecture Decision Records (ADRs).
        
        YOUR TASK:
        Generate a complete ADR based on the RFC and selected alternative provided below.
        
        RFC INFORMATION:
        Title: %s
        Description: %s
        
        COMMUNITY FEEDBACK:
        %s
        
        SELECTED ALTERNATIVE:
        Name: %s
        Description: %s
        Pros: %s
        Cons: %s
        
        CRITICAL INSTRUCTIONS - READ CAREFULLY:
        1. You MUST respond with ONLY a valid JSON object
        2. Do NOT include markdown code blocks, backticks, or any formatting
        3. Do NOT include any text before or after the JSON
        4. The JSON must have exactly these three fields: "context", "decision", "consequences"
        5. All three fields MUST contain a maximum 2 sentences each. Be concise. This is a HARD REQUIREMENT.
        
        CONTENT REQUIREMENTS:
        
        "context": 
        - Explain the problem or need that led to this decision
        - Describe the business or technical requirements
        - Include relevant constraints and considerations
        - Incorporate insights from community comments if available
        
        "decision": 
        - State clearly which alternative was chosen
        - Explain the main reasons for this choice
        - Reference specific pros that influenced the decision
        - Address how this alternative meets the requirements
        
        "consequences":
        - List positive outcomes (benefits from the pros)
        - List negative outcomes or trade-offs (from the cons)
        - Mention risks or considerations raised in comments
        - Describe long-term implications
        
        REQUIRED OUTPUT FORMAT (copy this structure exactly):
        {"context":"your detailed context here","decision":"your detailed decision here","consequences":"your detailed consequences here"}
        
        Remember: Output ONLY the JSON object, nothing else. No explanations, no markdown, no code blocks.
        IT IS ABSOLUTELY CRITICAL that if information is missing, or contains information unrelated to software (i.e. "asdasd", gibberish, or the sort), you must return a message on all fields that there is insufficient information to generate the ADR.
        You must not make up any information and you must not generate an ADR if the RFC or alternative lacks sufficient detail.
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
            // Remove common formatting issues
            String json = response
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .replaceAll("^[^{]*", "") // Remove text before first {
                    .replaceAll("[^}]*$", "") // Remove text after last }
                    .trim();
            
            LlmAdrContent parsed = objectMapper.readValue(json, LlmAdrContent.class);
            
            // Validate parsed content
            if (parsed.context() == null || parsed.context().trim().isEmpty() ||
                parsed.decision() == null || parsed.decision().trim().isEmpty() ||
                parsed.consequences() == null || parsed.consequences().trim().isEmpty()) {
                throw new IllegalStateException("Parsed JSON missing required fields");
            }
            
            return parsed;
        } catch (Exception e) {
            log.error("Error in manual parsing. Raw response: " + response, e);
            // Last resort fallback with error message
            return new LlmAdrContent(
                    "Error: Unable to generate proper context. Please review the RFC and alternative manually.",
                    "Error: Unable to generate proper decision. Manual review required.",
                    "Error: Unable to generate proper consequences. Manual review required."
            );
        }
    }

    // LLM answer
    public record LlmAdrContent(String context, String decision, String consequences) {}
}