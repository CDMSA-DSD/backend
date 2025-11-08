package dsd.api.cdmsa.exception;

// Thrown when a user who is not the author of the RFC tries to create an alternative
public class RfcAlternativeNotAllowedException extends RuntimeException {
    public RfcAlternativeNotAllowedException(String message) {
        super(message);
    }
}
