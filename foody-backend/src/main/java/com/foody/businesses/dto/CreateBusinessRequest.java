package com.foody.businesses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/business — one-time self-registration of the authenticated owner's
 * business. Creates the row with status PENDING; use PATCH /api/business/profile
 * afterward for edits.
 */
public record CreateBusinessRequest(
        @NotBlank(message = "name is required") @Size(max = 255) String name,
        @NotBlank(message = "businessType is required") @Size(max = 50) String businessType,
        @Size(max = 2000) String description,
        @Size(max = 255) String address,
        @Size(max = 64) String phone) {
}
