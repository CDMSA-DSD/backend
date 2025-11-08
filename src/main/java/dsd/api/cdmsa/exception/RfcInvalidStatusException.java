package dsd.api.cdmsa.exception;

// Thrown when an opperation is attempted on an RFC with an invalid status (the RFC is closed)
public class RfcInvalidStatusException extends RuntimeException {
    public RfcInvalidStatusException(String message) {
        super(message);
    }

}
