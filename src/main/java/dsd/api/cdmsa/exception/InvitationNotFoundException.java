package dsd.api.cdmsa.exception;

public class InvitationNotFoundException extends RuntimeException{
    public InvitationNotFoundException(Long id){
        super("Invitation " + id + " not found.");
    }
}
