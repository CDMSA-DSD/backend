package dsd.api.cdmsa.service;

import dsd.api.cdmsa.model.ADR;
import dsd.api.cdmsa.model.RFC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.UUID;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final VectorStore vectorStore;

    public void ingestRFC(RFC rfc) {
        try {

            String content = buildRFCContent(rfc);
            Map<String, Object> metadata = buildRFCMetadata(rfc);

            String rawDocId = "RFC_" + rfc.getId();
            String docId = UUID.nameUUIDFromBytes(rawDocId.getBytes()).toString();

            Document doc = new Document(docId, content, metadata);

            vectorStore.add(List.of(doc));
            log.info("RFC {} ('{}') vectorized successfully", rfc.getId(), rfc.getTitle());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid RFC data: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Vectorization failed for RFC {}", rfc.getId(), e);
            throw new RuntimeException("Failed to ingest RFC: " + e.getMessage(), e);
        }
    }

    public void ingestADR(ADR adr) {
        try {

            String content = buildADRContent(adr);
            Map<String, Object> metadata = buildADRMetadata(adr);

            String rawDocId = "ADR_" + adr.getId();
            String docId = UUID.nameUUIDFromBytes(rawDocId.getBytes()).toString();

            Document doc = new Document(docId, content, metadata);

            vectorStore.add(List.of(doc));
            log.info("ADR {} ('{}') vectorized successfully", adr.getId(), adr.getTitle());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid ADR data: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Vectorization failed for ADR {}", adr.getId(), e);
            throw new RuntimeException("Failed to ingest ADR: " + e.getMessage(), e);
        }
    }

    private String buildRFCContent(RFC rfc) {
        StringBuilder content = new StringBuilder();
        content.append("=== RFC (Request for Comments) ===\n\n");
        content.append("TITLE: ").append(rfc.getTitle()).append("\n");
        content.append("STATUS: ").append(rfc.getStatus() != null ? rfc.getStatus() : "N/A").append("\n");

        if (rfc.getUser() != null) {
            content.append("AUTHOR: ")
                    .append(rfc.getUser().getFirstname())
                    .append(" ")
                    .append(rfc.getUser().getLastname())
                    .append("\n");
        }

        if (rfc.getCreatedAt() != null) {
            content.append("CREATION DATE: ").append(rfc.getCreatedAt()).append("\n");
        }

        content.append("\nDESCRIPTION:\n").append(rfc.getDescription());

        // Alternatives
        if (rfc.getAlternatives() != null && !rfc.getAlternatives().isEmpty()) {
            content.append("\n--- PROPOSED ALTERNATIVES ---\n");
            for (var alt : rfc.getAlternatives()) {
                String selectedMark = (Boolean.TRUE.equals(alt.getIsWinning())) ? " [SELECTED DECISION]" : "";

                content.append(String.format("\n>>> ALTERNATIVE: %s%s\n", alt.getTitle(), selectedMark));
                content.append("    Description: ").append(alt.getDescription()).append("\n");

                if (alt.getPros() != null && !alt.getPros().isBlank()) {
                    content.append("    Pros: ").append(alt.getPros()).append("\n");
                }
                if (alt.getCons() != null && !alt.getCons().isBlank()) {
                    content.append("    Cons: ").append(alt.getCons()).append("\n");
                }
            }
        }

        // Comments
        if (rfc.getComments() != null && !rfc.getComments().isEmpty()) {
            content.append("\n--- TEAM DISCUSSION / COMMENTS ---\n");
            for (var comment : rfc.getComments()) {
                String author = (comment.getAuthor() != null)
                        ? comment.getAuthor().getFirstname() + " " + comment.getAuthor().getLastname()
                        : "Unknown User";
                content.append(String.format("- %s wrote: %s\n", author, comment.getContent()));
            }
        }

        // Add other things?????

        return content.toString();
    }

    private String buildADRContent(ADR adr) {
        StringBuilder content = new StringBuilder();
        content.append("=== ADR (Architectural Decision Record) ===\n\n");
        content.append("TITLE: ").append(adr.getTitle()).append("\n");
        content.append("STATUS: ").append(adr.getStatus()).append("\n");

        if (adr.getCreatedAt() != null) {
            content.append("DATA: ").append(adr.getCreatedAt()).append("\n");
        }

        content.append("\nCONTEXT:\n").append(adr.getContext()).append("\n");
        content.append("\nDECISION:\n").append(adr.getDecision());

        if (adr.getConsequences() != null && !adr.getConsequences().isBlank()) {
            content.append("\n\nCONSEQUENCES:\n").append(adr.getConsequences());
        }

        // Add other things?????

        return content.toString();
    }

    private Map<String, Object> buildRFCMetadata(RFC rfc) {
        return Map.of(
                "source_id", rfc.getId().toString(),
                "type", "RFC",
                "title", rfc.getTitle(),
                "status", rfc.getStatus() != null ? rfc.getStatus() : "N/A",
                "created_at", rfc.getCreatedAt() != null ? rfc.getCreatedAt().toString() : "",
                "org_id", rfc.getOrg().getId().toString()
        );
    }

    private Map<String, Object> buildADRMetadata(ADR adr) {
        return Map.of(
                "source_id", adr.getId().toString(),
                "type", "ADR",
                "title", adr.getTitle(),
                "status", adr.getStatus(),
                "created_at", adr.getCreatedAt() != null ? adr.getCreatedAt().toString() : "",
                "org_id", adr.getRfc().getOrg().getId().toString()
        );
    }

    public void deleteADR(Long adrId) {
        try {
            String rawDocId = "ADR_" + adrId;
            String docId = UUID.nameUUIDFromBytes(rawDocId.getBytes()).toString();
            vectorStore.delete(List.of(docId));
            log.info("Removed ADR {} from the vectorDB", adrId);
        } catch (Exception e) {
            log.error("Errore deleting vector ADR {}", adrId, e);
        }
    }
}