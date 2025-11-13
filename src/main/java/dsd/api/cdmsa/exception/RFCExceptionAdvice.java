package dsd.api.cdmsa.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = dsd.api.cdmsa.controller.RfcController.class)
public class RFCExceptionAdvice {

    @ExceptionHandler(RfcBadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(RfcBadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "RFC Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(RfcNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RfcNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "RFC Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(RfcDependencyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDependencyNotFound(RfcDependencyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Related Resource Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler({
            RfcAlternativeBadRequestException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler({
            RfcAlternativeNotAllowedException.class
    })
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Forbidden",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }

    @ExceptionHandler(RfcInvalidStatusException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(RfcInvalidStatusException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Invalid RFC status",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()));
    }
}
