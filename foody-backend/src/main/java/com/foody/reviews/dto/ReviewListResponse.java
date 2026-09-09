package com.foody.reviews.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReviewListResponse(List<ReviewResponse> reviews, BigDecimal averageRating, long reviewCount) {
}
