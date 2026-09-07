package com.foody.wallet.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="wallet_transactions") public class WalletTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="wallet_id",nullable=false) private Long walletId;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount; @Enumerated(EnumType.STRING) @Column(nullable=false) private WalletTransactionType type;
 @Column(name="actor_user_id",nullable=false) private Long actorUserId; @Enumerated(EnumType.STRING) @Column(name="actor_type",nullable=false) private WalletActorType actorType;
 @Column(name="balance_after",nullable=false,precision=12,scale=2) private BigDecimal balanceAfter; @Column(name="debit_request_id") private Long debitRequestId;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @PrePersist void create(){createdAt=Instant.now();}
 public Long getId(){return id;} public Long getWalletId(){return walletId;} public void setWalletId(Long v){walletId=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 public WalletTransactionType getType(){return type;} public void setType(WalletTransactionType v){type=v;} public Long getActorUserId(){return actorUserId;} public void setActorUserId(Long v){actorUserId=v;}
 public WalletActorType getActorType(){return actorType;} public void setActorType(WalletActorType v){actorType=v;} public BigDecimal getBalanceAfter(){return balanceAfter;} public void setBalanceAfter(BigDecimal v){balanceAfter=v;}
 public Long getDebitRequestId(){return debitRequestId;} public void setDebitRequestId(Long v){debitRequestId=v;} public Instant getCreatedAt(){return createdAt;}
}
