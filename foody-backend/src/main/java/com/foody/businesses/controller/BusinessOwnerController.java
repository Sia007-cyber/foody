package com.foody.businesses.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.businesses.dto.BusinessResponse;
import com.foody.businesses.dto.CreateBusinessRequest;
import com.foody.businesses.dto.UpdateBusinessProfileRequest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business panel — profile endpoints, scoped to the authenticated owner's own
 * business. Phase 1 assumes one business per owner (see BusinessRepository note).
 */
@RestController
@RequestMapping("/api/business")
@PreAuthorize("hasRole('BUSINESS_OWNER')")
public class BusinessOwnerController {

    private final BusinessService businessService;

    public BusinessOwnerController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessResponse create(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                   @Valid @RequestBody CreateBusinessRequest request) {
        return BusinessResponse.from(businessService.createForOwner(principal.getUserId(), request));
    }

    @GetMapping("/profile")
    public BusinessResponse getProfile(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        Business business = businessService.findByOwnerUserId(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No business found for this owner"));
        return BusinessResponse.from(business);
    }

    @PatchMapping("/profile")
    public BusinessResponse updateProfile(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                          @Valid @RequestBody UpdateBusinessProfileRequest request) {
        return BusinessResponse.from(businessService.updateProfile(principal.getUserId(), request));
    }
}
