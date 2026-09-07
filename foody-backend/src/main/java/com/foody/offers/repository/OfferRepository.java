package com.foody.offers.repository;

import com.foody.offers.entity.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OfferRepository extends JpaRepository<Offer,Long> {
    List<Offer> findByBusinessIdOrderByCreatedAtDesc(Long businessId);
    Optional<Offer> findByIdAndBusinessId(Long id, Long businessId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Offer o where o.id=:id") Optional<Offer> findByIdForUpdate(@Param("id") Long id);
    @Query("select o from Offer o join Business b on b.id=o.businessId where b.status=com.foody.businesses.entity.BusinessStatus.APPROVED and o.status=com.foody.offers.entity.OfferStatus.ACTIVE and o.startsAt<=:now and o.expiresAt>:now and (select count(c) from OfferClaim c where c.offerId=o.id)<o.capacity order by o.createdAt desc")
    List<Offer> findClaimable(@Param("now") Instant now);
}
