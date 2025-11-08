package dsd.api.cdmsa.exception;

public class OrgNotFoundException extends RuntimeException{
    public OrgNotFoundException(Integer id){
        super("Organization " + id + " not found.");
    }
}
