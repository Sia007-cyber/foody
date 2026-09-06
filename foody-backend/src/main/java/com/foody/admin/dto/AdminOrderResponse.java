package com.foody.admin.dto;

import com.foody.orders.dto.OrderItemResponse;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.entity.FulfillmentType;
import com.foody.orders.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Admin-facing view of an order: the order itself plus the business and customer
 * names/contact info needed to display a cross-business list, since the plain
 * {@code OrderResponse} only carries their IDs. Kept separate from that record so
 * this module's response shape can evolve independently (see AdminBusinessResponse
 * for the same pattern).
 */
public record AdminOrderResponse(
        Long id,
        Long businessId,
        String businessName,
        Long customerUserId,
        String customerName,
        String customerEmail,
        String customerPhone,
        FulfillmentType fulfillmentType,
        OrderStatus status,
        String deliveryAddress,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminOrderResponse from(OrderResponse order, String businessName,
                                          String customerName, String customerEmail, String customerPhone) {
        return new AdminOrderResponse(
                order.id(), order.businessId(), businessName,
                order.customerUserId(), customerName, customerEmail, customerPhone,
                order.fulfillmentType(), order.status(), order.deliveryAddress(),
                order.totalAmount(), order.items(), order.createdAt(), order.updatedAt());
    }
}
