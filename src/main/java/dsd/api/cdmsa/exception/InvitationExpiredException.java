package dsd.api.cdmsa.exception;

public class InvitationExpiredException extends RuntimeException{
    public InvitationExpiredException(){
        super("This invitation has expired.");
    }
}
