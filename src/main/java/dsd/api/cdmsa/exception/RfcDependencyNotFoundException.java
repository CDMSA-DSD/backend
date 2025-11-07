package dsd.api.cdmsa.exception;

// When a required related entity is missing (user, template, org)
public class RfcDependencyNotFoundException extends RuntimeException {
    public RfcDependencyNotFoundException(String message) {
        super(message);
    }
}