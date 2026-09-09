package com.foody.reviews.dto;
import com.foody.reviews.entity.ProductReview; import java.time.Instant;
public record ProductReviewResponse(Long id,Integer rating,String comment,String reviewerDisplayName,Instant createdAt,Instant updatedAt){
 public static ProductReviewResponse from(ProductReview r,String name){return new ProductReviewResponse(r.getId(),r.getRating(),r.getComment(),name,r.getCreatedAt(),r.getUpdatedAt());}
}
