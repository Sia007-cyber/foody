package com.foody.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.admin.dto.AdminOrderResponse;
import com.foody.admin.dto.DashboardSummaryResponse;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.entity.FulfillmentType;
import com.foody.orders.entity.OrderStatus;
import com.foody.orders.service.OrderService;
import com.foody.reservations.service.ReservationService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import java.math.BigDecimal;
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
    static final Long USER_ID = 20L;

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

    private User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
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

    @Test
    void getUsers_delegatesToUserServiceWithFilters() {
        when(userService.findAll(UserRole.CUSTOMER, UserStatus.ACTIVE)).thenReturn(List.of(user()));

        List<User> result = adminService.getUsers(UserRole.CUSTOMER, UserStatus.ACTIVE);

        assertThat(result).hasSize(1);
    }

    @Test
    void suspendUser_delegatesToUserServiceWithSuspendedStatus() {
        User suspended = user();
        suspended.setStatus(UserStatus.SUSPENDED);
        when(userService.updateStatus(USER_ID, UserStatus.SUSPENDED)).thenReturn(suspended);

        User result = adminService.suspendUser(USER_ID);

        assertThat(result.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(userService).updateStatus(USER_ID, UserStatus.SUSPENDED);
    }

    @Test
    void activateUser_delegatesToUserServiceWithActiveStatus() {
        User active = user();
        when(userService.updateStatus(USER_ID, UserStatus.ACTIVE)).thenReturn(active);

        User result = adminService.activateUser(USER_ID);

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userService).updateStatus(USER_ID, UserStatus.ACTIVE);
    }

    @Test
    void getOrders_enrichesOrdersWithBusinessAndCustomerNames() {
        OrderResponse order = new OrderResponse(
                100L, BUSINESS_ID, USER_ID, FulfillmentType.PICKUP, OrderStatus.PENDING,
                null, new BigDecimal("9.00"), List.of(), null, null);

        Business business = business();
        business.setName("Test Cafe");

        User customer = user();
        customer.setFullName("Ali Rezaei");
        customer.setEmail("ali@example.com");
        customer.setPhone("0912");

        when(orderService.getAllOrders(OrderStatus.PENDING, null)).thenReturn(List.of(order));
        when(businessService.findAll(null)).thenReturn(List.of(business));
        when(userService.findAll(null, null)).thenReturn(List.of(customer));

        List<AdminOrderResponse> result = adminService.getOrders(OrderStatus.PENDING, null);

        assertThat(result).hasSize(1);
        AdminOrderResponse enriched = result.get(0);
        assertThat(enriched.businessName()).isEqualTo("Test Cafe");
        assertThat(enriched.customerName()).isEqualTo("Ali Rezaei");
        assertThat(enriched.customerEmail()).isEqualTo("ali@example.com");
    }

    @Test
    void getOrders_fallsBackToPlaceholderNamesWhenLookupMisses() {
        OrderResponse order = new OrderResponse(
                100L, 999L, 888L, FulfillmentType.PICKUP, OrderStatus.PENDING,
                null, new BigDecimal("9.00"), List.of(), null, null);

        when(orderService.getAllOrders(null, null)).thenReturn(List.of(order));
        when(businessService.findAll(null)).thenReturn(List.of());
        when(userService.findAll(null, null)).thenReturn(List.of());

        List<AdminOrderResponse> result = adminService.getOrders(null, null);

        assertThat(result.get(0).businessName()).isEqualTo("کسب‌وکار حذف‌شده");
        assertThat(result.get(0).customerName()).isEqualTo("کاربر حذف‌شده");
    }
}
