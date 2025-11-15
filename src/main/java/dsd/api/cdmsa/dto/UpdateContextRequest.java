package dsd.api.cdmsa.dto;

// Not sure if we should only be able to update the name, it made sense to me to 
// update types and descriptions as well

public record UpdateContextRequest(
        String name,
        String type, // to be confirmed
        String description // to be confirmed
) {
}
