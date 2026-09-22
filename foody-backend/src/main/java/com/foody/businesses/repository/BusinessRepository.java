package com.foody.businesses.repository;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByIdAndStatus(Long id, com.foody.businesses.entity.BusinessStatus status);

    // Phase 1 assumes one business per owner for the business panel; multi-business
    // ownership (if ever needed) would replace this with a List-returning finder.
    Optional<Business> findByOwnerUserId(Long ownerUserId);

    // Admin panel: businesses awaiting/holding a given approval status.
    List<Business> findByStatusOrderByCreatedAtDesc(BusinessStatus status);

    List<Business> findByStatusAndFeaturedTrueOrderByUpdatedAtDesc(BusinessStatus status);

    @Query("SELECT b FROM Business b LEFT JOIN com.foody.reviews.entity.Review r ON r.businessId = b.id AND r.moderationStatus = com.foody.reviews.entity.ReviewModerationStatus.APPROVED WHERE b.status = :status AND b.city = :city GROUP BY b.id ORDER BY CASE WHEN COUNT(r) = 0 THEN 1 ELSE 0 END, AVG(r.rating) DESC, b.name ASC, b.id ASC")
    List<Business> findPublicByCityOrderByRating(@Param("status") BusinessStatus status, @Param("city") String city);

    @Query("SELECT b FROM Business b LEFT JOIN com.foody.reviews.entity.Review r ON r.businessId = b.id AND r.moderationStatus = com.foody.reviews.entity.ReviewModerationStatus.APPROVED WHERE b.status = :status GROUP BY b.id ORDER BY CASE WHEN COUNT(r) = 0 THEN 1 ELSE 0 END, AVG(r.rating) DESC, b.name ASC, b.id ASC")
    List<Business> findPublicOrderByRating(@Param("status") BusinessStatus status);

    // Admin dashboard summary.
    long countByStatus(BusinessStatus status);

    // Phase 1 Discover: only APPROVED businesses, optionally filtered by exact
    // business_type code and/or a case-insensitive substring match on name.
    // Passing null for a param means "don't filter on it".
    @Query("""
            SELECT b FROM Business b
            WHERE b.status = :status
              AND (:type IS NULL OR b.businessType = :type)
              AND (:search IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY b.createdAt DESC
            """)
    List<Business> search(@Param("status") BusinessStatus status,
                           @Param("type") String type,
                           @Param("search") String search);
}
