package dsd.api.cdmsa.exception;

public class OrgNotFoundException extends RuntimeException{
    public OrgNotFoundException(Long id){
        super("Organization " + id + " not found.");
    }
}
