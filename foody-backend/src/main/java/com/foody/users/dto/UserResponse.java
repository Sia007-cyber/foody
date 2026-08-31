package com.foody.users.dto;

import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.time.Instant;

/**
 * Public projection of a user. Returned from controllers instead of the entity.
 * Deliberately omits passwordHash and other internals.
 */
public record UserResponse(
        Long id,
        String email,
        String phone,
        String fullName,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getPhone(),
                u.getFullName(),
                u.getRole(),
                u.getStatus(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
