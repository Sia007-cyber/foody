package com.foody.reviews.dto;

import com.foody.reviews.entity.ReviewModerationStatus;
import java.time.Instant;

public record AdminReviewResponse(Long id, String reviewType, String reviewerDisplayName,
                                  String businessName, String targetName, Integer rating,
                                  String comment, ReviewModerationStatus moderationStatus,
                                  Instant createdAt) {}
