package com.foody.admin.dto;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.users.dto.UserResponse;

public record AdminUserDetailResponse(UserResponse user, OwnedBusiness ownedBusiness) {
    public record OwnedBusiness(Long id, String name, BusinessStatus status, String managerNationalId) {
        public static OwnedBusiness from(Business business) {
            return new OwnedBusiness(business.getId(), business.getName(), business.getStatus(),
                    business.getManagerNationalId());
        }
    }
}
