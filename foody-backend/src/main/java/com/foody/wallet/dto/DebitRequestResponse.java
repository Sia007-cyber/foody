package com.foody.wallet.dto;
import com.foody.wallet.entity.*; import java.math.BigDecimal; import java.time.Instant;
public record DebitRequestResponse(Long id,Long walletId,Long customerUserId,Long businessId,Long requestedByOwnerUserId,BigDecimal amount,DebitRequestStatus status,Instant createdAt,Instant resolvedAt){
 public static DebitRequestResponse from(OwnerDebitRequest r){return new DebitRequestResponse(r.getId(),r.getWalletId(),r.getCustomerUserId(),r.getBusinessId(),r.getRequestedByOwnerUserId(),r.getAmount(),r.getStatus(),r.getCreatedAt(),r.getResolvedAt());}}
