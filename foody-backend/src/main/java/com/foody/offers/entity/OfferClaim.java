package com.foody.offers.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "offer_claims", uniqueConstraints = @UniqueConstraint(name = "uk_offer_claims_offer_customer", columnNames = {"offer_id", "customer_user_id"}))
public class OfferClaim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "offer_id", nullable = false, updatable = false) private Long offerId;
    @Column(name = "customer_user_id", nullable = false, updatable = false) private Long customerUserId;
    @Column(name = "claimed_at", nullable = false, updatable = false) private Instant claimedAt;
    @PrePersist void onCreate(){if(claimedAt==null) claimedAt=Instant.now();}
    public Long getId(){return id;} public Long getOfferId(){return offerId;} public void setOfferId(Long v){offerId=v;}
    public Long getCustomerUserId(){return customerUserId;} public void setCustomerUserId(Long v){customerUserId=v;} public Instant getClaimedAt(){return claimedAt;}
}
