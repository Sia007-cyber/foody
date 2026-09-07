package com.foody.wallet.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="owner_debit_requests") public class OwnerDebitRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="wallet_id",nullable=false) private Long walletId;
 @Column(name="customer_user_id",nullable=false) private Long customerUserId; @Column(name="business_id",nullable=false) private Long businessId;
 @Column(name="requested_by_owner_user_id",nullable=false) private Long requestedByOwnerUserId; @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private DebitRequestStatus status=DebitRequestStatus.PENDING;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="resolved_at") private Instant resolvedAt; @PrePersist void create(){createdAt=Instant.now();}
 public Long getId(){return id;} public Long getWalletId(){return walletId;} public void setWalletId(Long v){walletId=v;} public Long getCustomerUserId(){return customerUserId;} public void setCustomerUserId(Long v){customerUserId=v;}
 public Long getBusinessId(){return businessId;} public void setBusinessId(Long v){businessId=v;} public Long getRequestedByOwnerUserId(){return requestedByOwnerUserId;} public void setRequestedByOwnerUserId(Long v){requestedByOwnerUserId=v;}
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public DebitRequestStatus getStatus(){return status;} public void setStatus(DebitRequestStatus v){status=v;}
 public Instant getCreatedAt(){return createdAt;} public Instant getResolvedAt(){return resolvedAt;} public void setResolvedAt(Instant v){resolvedAt=v;}
}
