package com.foody.reviews.repository;
import com.foody.reviews.entity.ProductReview; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ProductReviewRepository extends JpaRepository<ProductReview,Long>{
 List<ProductReview> findByProductIdAndModerationStatusOrderByCreatedAtDescIdDesc(Long productId,com.foody.reviews.entity.ReviewModerationStatus status);
 List<ProductReview> findByModerationStatusOrderByCreatedAtDescIdDesc(com.foody.reviews.entity.ReviewModerationStatus status);
 Optional<ProductReview> findByProductIdAndReviewerUserId(Long productId,Long reviewerUserId);
 boolean existsByProductIdAndReviewerUserId(Long productId,Long reviewerUserId);
 List<ProductReview> findByProductIdInAndModerationStatusOrderByCreatedAtDescIdDesc(Collection<Long> productIds,com.foody.reviews.entity.ReviewModerationStatus status);
 @Query("select avg(r.rating) as averageRating,count(r) as reviewCount from ProductReview r where r.productId=:productId and r.moderationStatus=com.foody.reviews.entity.ReviewModerationStatus.APPROVED") ReviewRatingSummary summarizeApproved(@Param("productId")Long productId);
}
