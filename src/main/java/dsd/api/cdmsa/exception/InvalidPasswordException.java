package dsd.api.cdmsa.exception;

public class InvalidPasswordException extends RuntimeException{
    public InvalidPasswordException(){
        super("Password is incorrect");
    }
}
