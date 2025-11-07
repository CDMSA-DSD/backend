package dsd.api.cdmsa.exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Integer id){
        super("User " + id + " not found.");
    }
}
