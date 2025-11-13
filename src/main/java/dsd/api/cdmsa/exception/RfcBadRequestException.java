package dsd.api.cdmsa.exception;

// When the RFC creation request is invalid
public class RfcBadRequestException extends RuntimeException {
    public RfcBadRequestException(String message) {
        super(message);
    }
}
