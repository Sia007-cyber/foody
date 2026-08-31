package com.foody.admin.controller;

import com.foody.admin.dto.DashboardSummaryResponse;
import com.foody.admin.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin panel — platform-wide summary counters. */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminService adminService;

    public AdminDashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return adminService.getDashboardSummary();
    }
}
