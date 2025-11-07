package dsd.api.cdmsa.dto;

/**
 * Request payload for closing an RFC (US-22 & US-23).
 */
public record CloseRfcRequest(
        Long alternativeId, // optional, only for closing with decision
        String reason // optional, only for closing without decision
) {
}
