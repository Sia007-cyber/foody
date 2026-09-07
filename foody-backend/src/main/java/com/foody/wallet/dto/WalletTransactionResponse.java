package com.foody.wallet.dto;
import com.foody.wallet.entity.*; import java.math.BigDecimal; import java.time.Instant;
public record WalletTransactionResponse(Long id,Long walletId,BigDecimal amount,WalletTransactionType type,Long actorUserId,WalletActorType actorType,BigDecimal balanceAfter,Long debitRequestId,Instant createdAt){
 public static WalletTransactionResponse from(WalletTransaction t){return new WalletTransactionResponse(t.getId(),t.getWalletId(),t.getAmount(),t.getType(),t.getActorUserId(),t.getActorType(),t.getBalanceAfter(),t.getDebitRequestId(),t.getCreatedAt());}}
