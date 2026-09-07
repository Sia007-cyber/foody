package com.foody.offers.service;

import com.foody.offers.dto.*;
import java.util.List;

public interface OfferService {
    OfferResponse create(Long ownerUserId, CreateOfferRequest request);
    List<OfferResponse> ownerOffers(Long ownerUserId);
    OfferResponse cancel(Long ownerUserId, Long offerId);
    List<OfferResponse> claimableOffers();
    OfferClaimResponse claim(Long customerUserId, Long offerId);
    List<OfferClaimResponse> myClaims(Long customerUserId);
}
