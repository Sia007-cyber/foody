package com.foody.offers.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "offers")
public class Offer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "business_id", nullable = false) private Long businessId;
    @Column(nullable = false) private String title;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private int capacity;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OfferStatus status = OfferStatus.ACTIVE;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId(){return id;} public Long getBusinessId(){return businessId;} public void setBusinessId(Long v){businessId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public int getCapacity(){return capacity;} public void setCapacity(int v){capacity=v;} public Instant getStartsAt(){return startsAt;} public void setStartsAt(Instant v){startsAt=v;}
    public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){expiresAt=v;} public OfferStatus getStatus(){return status;} public void setStatus(OfferStatus v){status=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public long getVersion(){return version;}
}
