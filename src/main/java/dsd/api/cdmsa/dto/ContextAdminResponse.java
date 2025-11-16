package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ContextMembership;

public record ContextAdminResponse(
        Long userId,
        String username,
        boolean contextAdmin) {

    public static ContextAdminResponse fromMembership(ContextMembership member) {
        return new ContextAdminResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.isContextAdmin());
    }
}
