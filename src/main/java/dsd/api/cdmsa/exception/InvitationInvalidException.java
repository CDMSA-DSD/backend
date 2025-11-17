package dsd.api.cdmsa.exception;

public class InvitationInvalidException extends RuntimeException{
    public InvitationInvalidException(){
        super("Invalid invitation token");
    }
}
