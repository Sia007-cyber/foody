package com.foody.businesses.controller;

import com.foody.businesses.dto.BusinessResponse;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 0 exposed a read-only single-business lookup so the seeded demo business
 * (V2 migration) was viewable via API. Phase 1 adds the Discover listing
 * (GET /api/businesses?type=&search=) per the spec — type filter + simple
 * case-insensitive name search over APPROVED businesses only. lat/lng-based
 * sorting/filtering is explicitly out of scope for Phase 1.
 */
@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping
    public List<BusinessResponse> discover(@RequestParam(required = false) String type,
                                            @RequestParam(required = false) String search) {
        return businessService.search(type, search).stream()
                .map(BusinessResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public BusinessResponse getById(@PathVariable Long id) {
        Business business = businessService.findByIdAndStatus(id, BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
        return BusinessResponse.from(business);
    }
}
