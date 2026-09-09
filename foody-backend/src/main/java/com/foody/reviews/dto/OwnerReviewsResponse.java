package com.foody.reviews.dto;
import java.util.List;
public record OwnerReviewsResponse(List<ReviewResponse> businessReviews,List<OwnerProductReviewResponse> productReviews){}
