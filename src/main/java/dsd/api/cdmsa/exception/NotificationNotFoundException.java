package dsd.api.cdmsa.exception;

public class NotificationNotFoundException extends RuntimeException{
    public NotificationNotFoundException(Long id){
        super("Notification " + id + " not found.");
    }
}
