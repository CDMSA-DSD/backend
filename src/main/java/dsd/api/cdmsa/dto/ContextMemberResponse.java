package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ContextMembership;

public record ContextMemberResponse(
        Long userId,
        String firstname,
        String lastname,
        String email,
        boolean contextAdmin) {
    public static ContextMemberResponse fromMembership(ContextMembership membership) {
        return new ContextMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getFirstname(),
                membership.getUser().getLastname(),
                membership.getUser().getEmail(),
                membership.isContextAdmin());
    }
}
