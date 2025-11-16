package dsd.api.cdmsa.mapper;

import dsd.api.cdmsa.dto.UserResponse;
import dsd.api.cdmsa.model.User;

public class UserMapper {

    // Map User entity to UserResponse orgId or password
    public static UserResponse toDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail());
    }
}
