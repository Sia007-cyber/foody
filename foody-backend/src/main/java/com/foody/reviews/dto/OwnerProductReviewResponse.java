package com.foody.reviews.dto;
import java.time.Instant;
public record OwnerProductReviewResponse(Long id,Long productId,String productName,Integer rating,String comment,String reviewerDisplayName,Instant createdAt,Instant updatedAt){}
