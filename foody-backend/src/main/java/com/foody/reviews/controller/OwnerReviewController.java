package com.foody.reviews.controller;
import com.foody.auth.security.FoodyUserPrincipal; import com.foody.reviews.dto.OwnerReviewsResponse; import com.foody.reviews.service.ProductReviewService; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/business/reviews") @PreAuthorize("hasRole('BUSINESS_OWNER')")
public class OwnerReviewController {private final ProductReviewService reviews;public OwnerReviewController(ProductReviewService r){reviews=r;}@GetMapping public OwnerReviewsResponse list(@AuthenticationPrincipal FoodyUserPrincipal p){return reviews.ownerReviews(p.getUserId());}}
