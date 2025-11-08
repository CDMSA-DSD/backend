package dsd.api.cdmsa.exception;

import java.util.HashMap;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice //Catches exceptions of all the app and returns JSON
public class OrgExceptionAdvice {
    @ExceptionHandler(OrgNotFoundException.class) // Catches a specific exception and return a message
    @ResponseStatus(HttpStatus.NOT_FOUND) // Code returned
    ErrorMessage OrgExceptionAdviceNotFoundHandler(OrgNotFoundException ex) {
        return new ErrorMessage(ex.getMessage());
    }

    @ExceptionHandler(OrgExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorMessage OrgExceptionAdviceExistsHandler(OrgExistsException ex) {
        return new ErrorMessage(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleValidationException (MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap <>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ErrorMessage(errors.toString());
    }
}
