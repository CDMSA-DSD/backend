package dsd.api.cdmsa.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = dsd.api.cdmsa.controller.ContextController.class)

public class ContextExceptionAdvice {

    @ExceptionHandler(ContextNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContextNotFound(ContextNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "error", "Context Not Found",
                "message", ex.getMessage(),
                "timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ContextForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleContextForbidden(ContextForbiddenException ex) {
        Map<String, Object> body = Map.of(
                "error", "Forbidden",
                "message", ex.getMessage(),
                "timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ContextBadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleContextBadRequest(ContextBadRequestException ex) {
        Map<String, Object> body = Map.of(
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

}
