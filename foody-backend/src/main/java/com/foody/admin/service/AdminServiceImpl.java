package com.foody.admin.service;

import com.foody.admin.dto.DashboardSummaryResponse;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.orders.service.OrderService;
import com.foody.reservations.service.ReservationService;
import com.foody.users.service.UserService;
import java.util.List;
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
    public DashboardSummaryResponse getDashboardSummary() {
        return new DashboardSummaryResponse(
                userService.count(),
                businessService.countByStatus(BusinessStatus.APPROVED),
                orderService.countAll(),
                reservationService.countAll());
    }
}
