package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ContextMembership;

public record ContextMemberResponse(
        Long userId,
        String username,
        String email,
        boolean contextAdmin) {
    public static ContextMemberResponse fromMembership(ContextMembership membership) {
        return new ContextMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.isContextAdmin());
    }
}
