package com.foody.admin.service;

import com.foody.admin.dto.AdminOrderResponse;
import com.foody.admin.dto.DashboardSummaryResponse;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.entity.OrderStatus;
import com.foody.orders.service.OrderService;
import com.foody.reservations.service.ReservationService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminServiceImpl implements AdminService {

    private final BusinessService businessService;
    private final UserService userService;
    private final OrderService orderService;
    private final ReservationService reservationService;

    AdminServiceImpl(BusinessService businessService, UserService userService,
                     OrderService orderService, ReservationService reservationService) {
        this.businessService = businessService;
        this.userService = userService;
        this.orderService = orderService;
        this.reservationService = reservationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Business> getBusinesses(BusinessStatus statusFilter) {
        return businessService.findAll(statusFilter);
    }

    @Override
    @Transactional
    public Business approveBusiness(Long businessId) {
        return businessService.updateStatus(businessId, BusinessStatus.APPROVED);
    }

    @Override
    @Transactional
    public Business rejectBusiness(Long businessId) {
        return businessService.updateStatus(businessId, BusinessStatus.REJECTED);
    }

    @Override
    @Transactional
    public Business suspendBusiness(Long businessId) {
        return businessService.updateStatus(businessId, BusinessStatus.SUSPENDED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers(UserRole roleFilter, UserStatus statusFilter) {
        return userService.findAll(roleFilter, statusFilter);
    }

    @Override
    @Transactional
    public User suspendUser(Long userId) {
        return userService.updateStatus(userId, UserStatus.SUSPENDED);
    }

    @Override
    @Transactional
    public User activateUser(Long userId) {
        return userService.updateStatus(userId, UserStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        return new DashboardSummaryResponse(
                userService.count(),
                businessService.countByStatus(BusinessStatus.APPROVED),
                orderService.countAll(),
                reservationService.countAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getOrders(OrderStatus statusFilter, Long businessIdFilter) {
        List<OrderResponse> orders = orderService.getAllOrders(statusFilter, businessIdFilter);

        // Phase 1 has no pagination anywhere in the app, so order volume stays small.
        // Two bulk lookups here beat one businesses/users round trip per order.
        Map<Long, String> businessNamesById = businessService.findAll(null).stream()
                .collect(Collectors.toMap(Business::getId, Business::getName));
        Map<Long, User> usersById = userService.findAll(null, null).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return orders.stream()
                .map(order -> {
                    String businessName = businessNamesById.getOrDefault(order.businessId(), "کسب‌وکار حذف‌شده");
                    User customer = usersById.get(order.customerUserId());
                    String customerName = customer != null ? customer.getFullName() : "کاربر حذف‌شده";
                    String customerEmail = customer != null ? customer.getEmail() : null;
                    String customerPhone = customer != null ? customer.getPhone() : null;
                    return AdminOrderResponse.from(order, businessName, customerName, customerEmail, customerPhone);
                })
                .toList();
    }
}
