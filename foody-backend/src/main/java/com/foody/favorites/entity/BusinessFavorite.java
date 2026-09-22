package com.foody.favorites.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "business_favorites", uniqueConstraints = @UniqueConstraint(name = "uk_business_favorites_customer_business", columnNames = {"customer_user_id", "business_id"}))
public class BusinessFavorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "customer_user_id", nullable = false) private Long customerUserId;
    @Column(name = "business_id", nullable = false) private Long businessId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getCustomerUserId() { return customerUserId; }
    public void setCustomerUserId(Long value) { customerUserId = value; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long value) { businessId = value; }
}
