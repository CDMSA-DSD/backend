package dsd.api.cdmsa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice //Catches exceptions of all the app and returns JSON
public class NotificationExceptionAdvice {
    @ExceptionHandler(NotificationNotFoundException.class) // Catches a specific exception and return a message
    @ResponseStatus(HttpStatus.NOT_FOUND) // Code returned
    ErrorMessage notificationNotFoundHandler(NotificationNotFoundException ex) {
        return new ErrorMessage(ex.getMessage());
    }
}
