package com.foody.offers.repository;

import com.foody.offers.entity.OfferClaim;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferClaimRepository extends JpaRepository<OfferClaim,Long> {
    long countByOfferId(Long offerId);
    boolean existsByOfferIdAndCustomerUserId(Long offerId, Long customerUserId);
    List<OfferClaim> findByCustomerUserIdOrderByClaimedAtDesc(Long customerUserId);
}
