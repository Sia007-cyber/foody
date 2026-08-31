package com.foody.admin.dto;

/**
 * GET /api/admin/dashboard/summary — platform-wide counters.
 * {@code activeBusinesses} counts only APPROVED businesses (i.e. visible in the app),
 * not PENDING/REJECTED/SUSPENDED ones.
 */
public record DashboardSummaryResponse(
        long totalUsers,
        long activeBusinesses,
        long totalOrders,
        long totalReservations) {
}
