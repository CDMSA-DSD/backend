package dsd.api.cdmsa.exception;

public class OrgExistsException extends RuntimeException{
    public OrgExistsException(String nombre){
        super("Organization named " + nombre + " already exists.");
    }
}
