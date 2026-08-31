package com.foody.users.dto;

import jakarta.validation.constraints.Size;

/** PATCH /api/users/me — only mutable self fields in Phase 0. */
public record UpdateProfileRequest(
        @Size(max = 255) String fullName,
        @Size(max = 64) String phone,
        @Size(min = 8, max = 128) String password) {
}
