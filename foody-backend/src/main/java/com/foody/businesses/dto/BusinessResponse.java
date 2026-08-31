package com.foody.businesses.dto;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Public view of a business. Returned instead of the entity. */
public record BusinessResponse(
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

    public static BusinessResponse from(Business b) {
        return new BusinessResponse(
                b.getId(), b.getOwnerUserId(), b.getName(), b.getDescription(),
                b.getBusinessType(), b.getAddress(), b.getLatitude(), b.getLongitude(),
                b.getPhone(), b.getStatus(), b.getCoverImageUrl(),
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
