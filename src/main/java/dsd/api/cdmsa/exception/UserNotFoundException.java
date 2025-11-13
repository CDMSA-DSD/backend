package dsd.api.cdmsa.exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Long id){
        super("User " + id + " not found.");
    }
    public UserNotFoundException(String name){
        super("User " + name + " not found.");
    }
}
