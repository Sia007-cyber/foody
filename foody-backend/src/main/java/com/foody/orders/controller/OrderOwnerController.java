package com.foody.orders.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.dto.UpdateOrderStatusRequest;
import com.foody.orders.entity.OrderStatus;
import com.foody.orders.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Business panel — orders placed with the calling owner's own business. */
@RestController
@RequestMapping("/api/business/orders")
@PreAuthorize("hasRole('BUSINESS_OWNER')")
public class OrderOwnerController {

    private final OrderService orderService;

    public OrderOwnerController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getBusinessOrders(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                                 @RequestParam(required = false) OrderStatus status) {
        return orderService.getBusinessOrders(principal.getUserId(), status);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(principal.getUserId(), id, request.status());
    }
}
