package dsd.api.cdmsa.exception;

public class UserExistsException extends RuntimeException{
    public UserExistsException(String nombre){
        super("User named " + nombre + " already exists.");
    }
}
