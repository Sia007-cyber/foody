package com.foody.orders.dto;

import com.foody.orders.entity.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
    }
}
