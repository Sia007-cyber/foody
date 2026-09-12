package com.foody.wallet.dto;
import com.foody.wallet.entity.WalletPurchaseItem;
import java.math.BigDecimal;
public record PurchaseItemResponse(Long productId,String productName,BigDecimal unitPrice,int quantity,BigDecimal lineTotal){
 public static PurchaseItemResponse from(WalletPurchaseItem i){return new PurchaseItemResponse(i.getProductId(),i.getProductNameSnapshot(),i.getUnitPriceSnapshot(),i.getQuantity(),i.getLineTotal());}
}
