package dsd.api.cdmsa.exception;

import java.util.List;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Long id){
        super("User " + id + " not found.");
    }
    public UserNotFoundException(List<Long> id){
        super("Users" + id + "not found.");
    }
    public UserNotFoundException(String name){
        super("User " + name + " not found.");
    }
}
