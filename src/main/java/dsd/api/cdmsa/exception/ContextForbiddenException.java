package dsd.api.cdmsa.exception;

// Thrown when a non organization admin user tries to create a context
public class ContextForbiddenException extends RuntimeException {
    public ContextForbiddenException(String message) {
        super(message);
    }
}
