package com.foody.auth.dto;

import com.foody.users.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Phase 0 register only creates CUSTOMER accounts. */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 255) String fullName,
        String phone) {

    public UserRole role() {
        return UserRole.CUSTOMER;
    }
}
