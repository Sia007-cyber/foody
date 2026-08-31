package com.foody.orders.service;

import com.foody.orders.dto.CreateOrderRequest;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.entity.OrderStatus;
import java.util.List;

/**
 * Public contract for the orders module. Other modules depend on this interface only.
 */
public interface OrderService {

    OrderResponse createOrder(Long customerUserId, CreateOrderRequest request);

    OrderResponse getOrderForCustomer(Long orderId, Long customerUserId);

    List<OrderResponse> getMyOrders(Long customerUserId);

    OrderResponse cancelOrder(Long orderId, Long customerUserId);

    // Business panel: orders placed with the calling owner's own business.
    // statusFilter is optional — pass null to list all statuses.
    List<OrderResponse> getBusinessOrders(Long ownerUserId, OrderStatus statusFilter);

    OrderResponse updateOrderStatus(Long ownerUserId, Long orderId, OrderStatus newStatus);
}
