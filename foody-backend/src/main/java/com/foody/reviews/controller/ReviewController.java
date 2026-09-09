package com.foody.reviews.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.reviews.dto.*;
import com.foody.reviews.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/businesses/{businessId}/reviews")
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) { this.reviews = reviews; }

    @GetMapping
    public ReviewListResponse list(@PathVariable Long businessId) { return reviews.list(businessId); }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_OWNER')")
    public ReviewResponse mine(@PathVariable Long businessId,
                               @AuthenticationPrincipal FoodyUserPrincipal principal) {
        return reviews.mine(businessId, principal.getUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_OWNER')")
    public ReviewResponse create(@PathVariable Long businessId,
                                 @AuthenticationPrincipal FoodyUserPrincipal principal,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviews.create(businessId, principal.getUserId(), request);
    }

    @PatchMapping("/mine")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_OWNER')")
    public ReviewResponse update(@PathVariable Long businessId,
                                 @AuthenticationPrincipal FoodyUserPrincipal principal,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviews.update(businessId, principal.getUserId(), request);
    }

    @DeleteMapping("/mine")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_OWNER')")
    public void delete(@PathVariable Long businessId,
                       @AuthenticationPrincipal FoodyUserPrincipal principal) {
        reviews.delete(businessId, principal.getUserId());
    }
}
