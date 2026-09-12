package com.foody.wallet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallet_purchase_items")
public class WalletPurchaseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="debit_request_id", nullable=false) private Long debitRequestId;
    @Column(name="product_id") private Long productId;
    @Column(name="product_name_snapshot", nullable=false) private String productNameSnapshot;
    @Column(name="unit_price_snapshot", nullable=false, precision=12, scale=2) private BigDecimal unitPriceSnapshot;
    @Column(nullable=false) private int quantity;
    @Column(name="line_total", nullable=false, precision=12, scale=2) private BigDecimal lineTotal;
    public Long getId(){return id;} public Long getDebitRequestId(){return debitRequestId;} public void setDebitRequestId(Long v){debitRequestId=v;}
    public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;} public String getProductNameSnapshot(){return productNameSnapshot;} public void setProductNameSnapshot(String v){productNameSnapshot=v;}
    public BigDecimal getUnitPriceSnapshot(){return unitPriceSnapshot;} public void setUnitPriceSnapshot(BigDecimal v){unitPriceSnapshot=v;} public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
    public BigDecimal getLineTotal(){return lineTotal;} public void setLineTotal(BigDecimal v){lineTotal=v;}
}
