package dsd.api.cdmsa.exception;

import java.util.List;
import java.util.stream.Collectors;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Long id){
        super("User " + id + " not found.");
    }
    public UserNotFoundException(List<Long> id){
        super("Users " + id.stream().map(String::valueOf).collect(Collectors.joining(", ")) + " not found.");
    }
    public UserNotFoundException(String name){
        super("User " + name + " not found.");
    }
}
