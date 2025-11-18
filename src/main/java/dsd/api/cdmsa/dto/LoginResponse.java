package dsd.api.cdmsa.dto;

import java.util.List;

public record LoginResponse(
        UserResponse user,
        String token,
        boolean isAdmin,
        List<ContextByAdminResponse> contextIsAdmin
        ) {}
