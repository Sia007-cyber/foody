package com.foody.reviews.dto;

import com.foody.reviews.entity.Review;
import java.time.Instant;
import com.foody.reviews.entity.ReviewModerationStatus;

public record ReviewResponse(Long id, Integer rating, String comment, String reviewerDisplayName,
                             ReviewModerationStatus moderationStatus, Instant createdAt, Instant updatedAt) {
    public static ReviewResponse from(Review review, String displayName) {
        return new ReviewResponse(review.getId(), review.getRating(), review.getComment(), displayName, review.getModerationStatus(),
                review.getCreatedAt(), review.getUpdatedAt());
    }
}
