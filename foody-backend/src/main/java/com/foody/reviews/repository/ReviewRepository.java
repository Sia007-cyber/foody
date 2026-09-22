package com.foody.reviews.repository;

import com.foody.reviews.entity.Review;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBusinessIdAndModerationStatusOrderByCreatedAtDescIdDesc(Long businessId, com.foody.reviews.entity.ReviewModerationStatus status);
    List<Review> findByModerationStatusOrderByCreatedAtDescIdDesc(com.foody.reviews.entity.ReviewModerationStatus status);
    Optional<Review> findByBusinessIdAndCustomerUserId(Long businessId, Long customerUserId);
    boolean existsByBusinessIdAndCustomerUserId(Long businessId, Long customerUserId);
    @Query("select avg(r.rating) as averageRating, count(r) as reviewCount from Review r where r.businessId = :businessId and r.moderationStatus = com.foody.reviews.entity.ReviewModerationStatus.APPROVED")
    ReviewRatingSummary summarizeApproved(@Param("businessId") Long businessId);
}
