package com.foody.offers.dto;

import com.foody.offers.entity.OfferClaim;
import com.foody.offers.entity.Offer;
import com.foody.offers.entity.OfferStatus;
import java.time.Instant;

public record OfferClaimResponse(Long id, Long offerId, String offerTitle, String offerDescription,
                                 Long businessId, String businessName, Instant claimedAt,
                                 long remainingAvailability, Instant expiresAt, OfferStatus offerStatus) {
    public static OfferClaimResponse from(OfferClaim c, Offer o, String businessName, long remaining) {
        return new OfferClaimResponse(c.getId(), c.getOfferId(), o.getTitle(), o.getDescription(),
                o.getBusinessId(), businessName, c.getClaimedAt(), remaining, o.getExpiresAt(), o.getStatus());
    }
}
