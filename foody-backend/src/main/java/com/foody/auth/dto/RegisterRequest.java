package com.foody.auth.dto;

import com.foody.users.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration. Callers may sign up as either a CUSTOMER or a
 * BUSINESS_OWNER; ADMIN accounts can never be created through this endpoint
 * (enforced in AuthServiceImpl).
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 255) String fullName,
        String phone,
        @NotNull UserRole role) {
}
