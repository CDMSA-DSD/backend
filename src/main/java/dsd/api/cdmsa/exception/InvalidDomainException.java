package dsd.api.cdmsa.exception;


public class InvalidDomainException extends RuntimeException{
    public InvalidDomainException(){
        super("Email does not correspond with the organization domain.");
    }
}
