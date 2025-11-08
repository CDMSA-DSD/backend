package dsd.api.cdmsa.exception;

// When an RFC cannot be found by ID
public class RfcNotFoundException extends RuntimeException {
    public RfcNotFoundException(String message) {
        super(message);
    }
}
