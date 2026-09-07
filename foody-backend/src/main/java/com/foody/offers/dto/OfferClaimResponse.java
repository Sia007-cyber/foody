package com.foody.offers.dto;

import com.foody.offers.entity.OfferClaim;
import java.time.Instant;

public record OfferClaimResponse(Long id, Long offerId, Instant claimedAt, long remainingAvailability) {
    public static OfferClaimResponse from(OfferClaim c, long remaining) { return new OfferClaimResponse(c.getId(), c.getOfferId(), c.getClaimedAt(), remaining); }
}
