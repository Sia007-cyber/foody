package com.foody.reviews.dto;
import java.math.BigDecimal; import java.util.List;
public record ProductReviewListResponse(List<ProductReviewResponse> reviews,BigDecimal averageRating,long reviewCount){}
