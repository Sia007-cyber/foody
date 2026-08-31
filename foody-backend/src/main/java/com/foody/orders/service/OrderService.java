package com.foody.orders.service;

import com.foody.orders.dto.CreateOrderRequest;
import com.foody.orders.dto.OrderResponse;
import java.util.List;

/**
 * Public contract for the orders module. Other modules depend on this interface only.
 */
public interface OrderService {

    OrderResponse createOrder(Long customerUserId, CreateOrderRequest request);

    OrderResponse getOrderForCustomer(Long orderId, Long customerUserId);

    List<OrderResponse> getMyOrders(Long customerUserId);

    OrderResponse cancelOrder(Long orderId, Long customerUserId);
}
