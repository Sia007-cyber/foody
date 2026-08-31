package com.foody.orders.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.orders.dto.CreateOrderRequest;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing order endpoints. All routes require authentication (see
 * WebSecurityConfig — orders are not in PUBLIC_MATCHERS). Business-owner endpoints
 * for accepting/updating orders arrive separately with the business panel.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                     @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(principal.getUserId(), request);
    }

    @GetMapping("/my")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        return orderService.getMyOrders(principal.getUserId());
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                 @PathVariable Long id) {
        return orderService.getOrderForCustomer(id, principal.getUserId());
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                @PathVariable Long id) {
        return orderService.cancelOrder(id, principal.getUserId());
    }
}
