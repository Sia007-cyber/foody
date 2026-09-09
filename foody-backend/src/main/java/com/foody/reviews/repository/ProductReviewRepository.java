package com.foody.reviews.repository;
import com.foody.reviews.entity.ProductReview; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ProductReviewRepository extends JpaRepository<ProductReview,Long>{
 List<ProductReview> findByProductIdOrderByCreatedAtDescIdDesc(Long productId);
 Optional<ProductReview> findByProductIdAndReviewerUserId(Long productId,Long reviewerUserId);
 boolean existsByProductIdAndReviewerUserId(Long productId,Long reviewerUserId);
 List<ProductReview> findByProductIdInOrderByCreatedAtDescIdDesc(Collection<Long> productIds);
 @Query("select avg(r.rating) as averageRating,count(r) as reviewCount from ProductReview r where r.productId=:productId") ReviewRatingSummary summarize(@Param("productId")Long productId);
}
