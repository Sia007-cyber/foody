package com.foody.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.admin.dto.DashboardSummaryResponse;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.orders.service.OrderService;
import com.foody.reservations.service.ReservationService;
import com.foody.users.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock BusinessService businessService;
    @Mock UserService userService;
    @Mock OrderService orderService;
    @Mock ReservationService reservationService;

    AdminServiceImpl adminService;

    static final Long BUSINESS_ID = 10L;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(businessService, userService, orderService, reservationService);
    }

    private Business business() {
        Business business = new Business();
        business.setId(BUSINESS_ID);
        business.setStatus(BusinessStatus.PENDING);
        return business;
    }

    @Test
    void getBusinesses_delegatesToBusinessServiceWithFilter() {
        when(businessService.findAll(BusinessStatus.PENDING)).thenReturn(List.of(business()));

        List<Business> result = adminService.getBusinesses(BusinessStatus.PENDING);

        assertThat(result).hasSize(1);
    }

    @Test
    void approveBusiness_delegatesToBusinessServiceWithApprovedStatus() {
        Business approved = business();
        approved.setStatus(BusinessStatus.APPROVED);
        when(businessService.updateStatus(BUSINESS_ID, BusinessStatus.APPROVED)).thenReturn(approved);

        Business result = adminService.approveBusiness(BUSINESS_ID);

        assertThat(result.getStatus()).isEqualTo(BusinessStatus.APPROVED);
        verify(businessService).updateStatus(BUSINESS_ID, BusinessStatus.APPROVED);
    }

    @Test
    void rejectBusiness_delegatesToBusinessServiceWithRejectedStatus() {
        Business rejected = business();
        rejected.setStatus(BusinessStatus.REJECTED);
        when(businessService.updateStatus(BUSINESS_ID, BusinessStatus.REJECTED)).thenReturn(rejected);

        Business result = adminService.rejectBusiness(BUSINESS_ID);

        assertThat(result.getStatus()).isEqualTo(BusinessStatus.REJECTED);
        verify(businessService).updateStatus(BUSINESS_ID, BusinessStatus.REJECTED);
    }

    @Test
    void suspendBusiness_delegatesToBusinessServiceWithSuspendedStatus() {
        Business suspended = business();
        suspended.setStatus(BusinessStatus.SUSPENDED);
        when(businessService.updateStatus(BUSINESS_ID, BusinessStatus.SUSPENDED)).thenReturn(suspended);

        Business result = adminService.suspendBusiness(BUSINESS_ID);

        assertThat(result.getStatus()).isEqualTo(BusinessStatus.SUSPENDED);
        verify(businessService).updateStatus(BUSINESS_ID, BusinessStatus.SUSPENDED);
    }

    @Test
    void getDashboardSummary_aggregatesCountsFromAllModules() {
        when(userService.count()).thenReturn(42L);
        when(businessService.countByStatus(BusinessStatus.APPROVED)).thenReturn(7L);
        when(orderService.countAll()).thenReturn(123L);
        when(reservationService.countAll()).thenReturn(58L);

        DashboardSummaryResponse summary = adminService.getDashboardSummary();

        assertThat(summary.totalUsers()).isEqualTo(42L);
        assertThat(summary.activeBusinesses()).isEqualTo(7L);
        assertThat(summary.totalOrders()).isEqualTo(123L);
        assertThat(summary.totalReservations()).isEqualTo(58L);
    }
}
