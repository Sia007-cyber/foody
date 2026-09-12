package com.foody.wallet.dto;
import com.foody.wallet.entity.*; import java.math.BigDecimal; import java.time.Instant; import java.util.List;
public record DebitRequestResponse(Long id,Long walletId,Long customerUserId,Long businessId,String businessName,Long requestedByOwnerUserId,BigDecimal amount,DebitRequestStatus status,Instant createdAt,Instant resolvedAt,List<PurchaseItemResponse> items){
 public static DebitRequestResponse from(OwnerDebitRequest r){return from(r,null,List.of());}
 public static DebitRequestResponse from(OwnerDebitRequest r,String businessName,List<WalletPurchaseItem> items){return new DebitRequestResponse(r.getId(),r.getWalletId(),r.getCustomerUserId(),r.getBusinessId(),businessName,r.getRequestedByOwnerUserId(),r.getAmount(),r.getStatus(),r.getCreatedAt(),r.getResolvedAt(),items.stream().map(PurchaseItemResponse::from).toList());}}
