package com.foody.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** PATCH /api/users/me — mutable self fields for every role's profile page. */
public record UpdateProfileRequest(
        @Size(max = 255) String fullName,
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "09\\d{9}") String phone,
        @Size(max = 512) String address,
        BigDecimal latitude,
        BigDecimal longitude,
        @Size(max = 512) String profileImageUrl,
        @Size(min = 8, max = 128) String password) {
}
