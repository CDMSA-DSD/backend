package dsd.api.cdmsa.dto;

import dsd.api.cdmsa.model.ContextMembership;

public record ContextByAdminResponse(
        Long contextId,
        String name) {

    public static ContextByAdminResponse fromMembership(ContextMembership member) {
        return new ContextByAdminResponse(
                member.getContext().getId(),
                member.getContext().getName());
    }
}
