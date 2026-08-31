package com.foody.admin.controller;

import com.foody.admin.dto.AdminBusinessResponse;
import com.foody.admin.service.AdminService;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin panel — business moderation. Approve/reject a PENDING business, or
 * suspend an already-APPROVED one. See {@code BusinessServiceImpl} for the
 * allowed status transitions.
 */
@RestController
@RequestMapping("/api/admin/businesses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBusinessController {

    private final AdminService adminService;

    public AdminBusinessController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<AdminBusinessResponse> getBusinesses(@RequestParam(required = false) BusinessStatus status) {
        return adminService.getBusinesses(status).stream()
                .map(AdminBusinessResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/approve")
    public AdminBusinessResponse approve(@PathVariable Long id) {
        Business business = adminService.approveBusiness(id);
        return AdminBusinessResponse.from(business);
    }

    @PatchMapping("/{id}/reject")
    public AdminBusinessResponse reject(@PathVariable Long id) {
        Business business = adminService.rejectBusiness(id);
        return AdminBusinessResponse.from(business);
    }

    @PatchMapping("/{id}/suspend")
    public AdminBusinessResponse suspend(@PathVariable Long id) {
        Business business = adminService.suspendBusiness(id);
        return AdminBusinessResponse.from(business);
    }
}
