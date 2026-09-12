package com.foody.wallet.dto;
import java.math.BigDecimal; import java.time.Instant; import java.util.List;
public record PurchaseHistoryResponse(Long id,Long businessId,String businessName,Long customerUserId,String customerDisplayName,Instant completedAt,BigDecimal totalAmount,List<PurchaseItemResponse> items) {}
