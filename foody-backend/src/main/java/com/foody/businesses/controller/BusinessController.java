package com.foody.businesses.controller;

import com.foody.businesses.dto.BusinessResponse;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 0 exposes a read-only business lookup so the seeded demo business
 * (V2 migration) is viewable via API. Full discovery/filtering arrives in Phase 1.
 */
@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/{id}")
    public BusinessResponse getById(@PathVariable Long id) {
        Business business = businessService.findByIdAndStatus(id, BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
        return BusinessResponse.from(business);
    }
}
