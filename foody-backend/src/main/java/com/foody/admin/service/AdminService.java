package com.foody.admin.service;

import com.foody.admin.dto.DashboardSummaryResponse;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import java.util.List;

/**
 * Public contract for the admin module. Composes the businesses/users/orders/
 * reservations modules through their service interfaces only.
 */
public interface AdminService {

    // statusFilter is optional — pass null to list businesses in any status.
    List<Business> getBusinesses(BusinessStatus statusFilter);

    Business approveBusiness(Long businessId);

    Business rejectBusiness(Long businessId);

    Business suspendBusiness(Long businessId);

    DashboardSummaryResponse getDashboardSummary();
}
