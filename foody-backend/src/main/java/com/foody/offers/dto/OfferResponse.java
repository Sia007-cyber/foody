package com.foody.offers.dto;

import com.foody.offers.entity.*;
import java.time.Instant;

public record OfferResponse(Long id, Long businessId, String businessName, String title, String description,
                            int capacity, long claimCount, long remainingAvailability, Instant startsAt,
                            Instant expiresAt, OfferStatus status, Instant createdAt, Instant updatedAt) {
    public static OfferResponse from(Offer o, String businessName, long claims) {
        return new OfferResponse(o.getId(), o.getBusinessId(), businessName, o.getTitle(), o.getDescription(),
                o.getCapacity(), claims, Math.max(0, o.getCapacity()-claims), o.getStartsAt(), o.getExpiresAt(),
                o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
