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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.foody.common.storage.ImageUploadService;
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
    private final ImageUploadService imageUploadService;

    public BusinessOwnerController(BusinessService businessService, ImageUploadService imageUploadService) {
        this.businessService = businessService;
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(value = "/profile/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BusinessResponse replaceCoverImage(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                              @RequestParam("file") MultipartFile file) {
        var upload = imageUploadService.store(ImageUploadService.UploadCategory.BUSINESS_COVER, file);
        try {
            var replacement = businessService.replaceCoverImage(principal.getUserId(), upload.publicUrl());
            try { imageUploadService.deleteManagedUrl(replacement.previousUrl()); }
            catch (RuntimeException ignored) { /* committed replacement remains valid */ }
            return BusinessResponse.from(replacement.value());
        } catch (RuntimeException ex) {
            try { imageUploadService.deleteManagedUrl(upload.publicUrl()); }
            catch (RuntimeException cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
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
