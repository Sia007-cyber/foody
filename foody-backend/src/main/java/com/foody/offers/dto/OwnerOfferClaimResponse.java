package com.foody.offers.dto;

import com.foody.offers.entity.OfferClaim;
import com.foody.users.entity.User;
import java.time.Instant;

public record OwnerOfferClaimResponse(Long id, Long offerId, String customerPublicId,
                                      String customerDisplayName, Instant claimedAt) {
    public static OwnerOfferClaimResponse from(OfferClaim claim, User customer) {
        return new OwnerOfferClaimResponse(claim.getId(), claim.getOfferId(), customer.getPublicId(),
                customer.getFullName(), claim.getClaimedAt());
    }
}
