package com.foody.orders.dto;

import com.foody.orders.entity.FulfillmentType;
import com.foody.orders.entity.Order;
import com.foody.orders.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Public view of an order. Returned instead of the entity. */
public record OrderResponse(
        Long id,
        Long businessId,
        Long customerUserId,
        FulfillmentType fulfillmentType,
        OrderStatus status,
        String deliveryAddress,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(), order.getBusinessId(), order.getCustomerUserId(),
                order.getFulfillmentType(), order.getStatus(), order.getDeliveryAddress(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
