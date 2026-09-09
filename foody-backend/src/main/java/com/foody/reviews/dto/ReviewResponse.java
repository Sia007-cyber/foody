package com.foody.reviews.dto;

import com.foody.reviews.entity.Review;
import java.time.Instant;

public record ReviewResponse(Long id, Integer rating, String comment, String reviewerDisplayName,
                             Instant createdAt, Instant updatedAt) {
    public static ReviewResponse from(Review review, String displayName) {
        return new ReviewResponse(review.getId(), review.getRating(), review.getComment(), displayName,
                review.getCreatedAt(), review.getUpdatedAt());
    }
}
