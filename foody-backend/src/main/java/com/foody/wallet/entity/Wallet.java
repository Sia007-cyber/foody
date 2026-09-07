package com.foody.wallet.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="wallets",uniqueConstraints=@UniqueConstraint(columnNames={"customer_user_id","business_id"}))
public class Wallet {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="customer_user_id",nullable=false) private Long customerUserId;
 @Column(name="business_id",nullable=false) private Long businessId;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal balance=BigDecimal.ZERO;
 @Version @Column(nullable=false) private Long version;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @PrePersist void create(){createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public Long getCustomerUserId(){return customerUserId;} public void setCustomerUserId(Long v){customerUserId=v;}
 public Long getBusinessId(){return businessId;} public void setBusinessId(Long v){businessId=v;} public BigDecimal getBalance(){return balance;}
 public void setBalance(BigDecimal v){balance=v;} public Long getVersion(){return version;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
