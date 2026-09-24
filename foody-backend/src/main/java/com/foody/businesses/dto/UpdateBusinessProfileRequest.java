package com.foody.businesses.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** PATCH /api/business/profile — only non-null fields are applied. */
public record UpdateBusinessProfileRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Size(max = 255) String address,
        BigDecimal latitude,
        BigDecimal longitude,
        @Size(max = 64) String phone,
        @Size(max = 512) String coverImageUrl) {
}
