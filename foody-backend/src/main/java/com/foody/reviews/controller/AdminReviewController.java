package com.foody.reviews.controller;

import com.foody.reviews.service.ReviewService;
import com.foody.reviews.service.ProductReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {
    private final ReviewService reviews;
    private final ProductReviewService productReviews;
    public AdminReviewController(ReviewService reviews, ProductReviewService productReviews) { this.reviews = reviews; this.productReviews = productReviews; }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { reviews.deleteForAdmin(id); }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProductReview(@PathVariable Long id) { productReviews.deleteForAdmin(id); }
}
