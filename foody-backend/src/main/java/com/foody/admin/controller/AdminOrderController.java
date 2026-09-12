package com.foody.admin.controller;

import com.foody.admin.dto.AdminOrderResponse;
import com.foody.admin.service.AdminService;
import com.foody.orders.entity.OrderStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin panel — read-only overview of orders across every business, optionally
 * filtered by status and/or business. Status changes stay owner-only; see
 * {@code OrderOwnerController} for the actual lifecycle transitions.
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@org.springframework.context.annotation.Profile("legacy-orders")
public class AdminOrderController {

    private final AdminService adminService;

    public AdminOrderController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<AdminOrderResponse> getOrders(@RequestParam(required = false) OrderStatus status,
                                              @RequestParam(required = false) Long businessId) {
        return adminService.getOrders(status, businessId);
    }
}
