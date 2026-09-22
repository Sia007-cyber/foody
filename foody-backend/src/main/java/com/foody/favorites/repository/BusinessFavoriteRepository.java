package com.foody.favorites.repository;

import com.foody.favorites.entity.BusinessFavorite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessFavoriteRepository extends JpaRepository<BusinessFavorite, Long> {
    void deleteByCustomerUserIdAndBusinessId(Long customerUserId, Long businessId);

    @Modifying
    @Query(value = "INSERT INTO business_favorites (customer_user_id, business_id, created_at) VALUES (:customerUserId, :businessId, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE id = id", nativeQuery = true)
    void addIfAbsent(@Param("customerUserId") Long customerUserId, @Param("businessId") Long businessId);
}
