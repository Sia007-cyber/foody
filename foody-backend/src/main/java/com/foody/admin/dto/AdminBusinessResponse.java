package com.foody.admin.dto;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Admin-facing view of a business. Kept separate from
 * {@code com.foody.businesses.dto.BusinessResponse} so the admin module's response
 * shape can evolve independently (e.g. adding owner contact info later) without
 * touching the businesses module's own public API.
 */
public record AdminBusinessResponse(
        Long id,
        Long ownerUserId,
        String name,
        String description,
        String businessType,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        BusinessStatus status,
        String coverImageUrl,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminBusinessResponse from(Business b) {
        return new AdminBusinessResponse(
                b.getId(), b.getOwnerUserId(), b.getName(), b.getDescription(),
                b.getBusinessType(), b.getAddress(), b.getLatitude(), b.getLongitude(),
                b.getPhone(), b.getStatus(), b.getCoverImageUrl(),
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
