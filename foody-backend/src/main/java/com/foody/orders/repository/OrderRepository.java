package com.foody.orders.repository;

import com.foody.orders.entity.Order;
import com.foody.orders.entity.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndCustomerUserId(Long id, Long customerUserId);

    List<Order> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);

    Optional<Order> findByIdAndBusinessId(Long id, Long businessId);

    List<Order> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    List<Order> findByBusinessIdAndStatusOrderByCreatedAtDesc(Long businessId, OrderStatus status);

    // Admin panel: orders across every business. Both filters are optional —
    // pass null for either to skip it (JPQL, not a derived query, since Spring Data
    // can't express "optional equality" via method-name conventions).
    @Query("SELECT o FROM Order o "
            + "WHERE (:status IS NULL OR o.status = :status) "
            + "AND (:businessId IS NULL OR o.businessId = :businessId) "
            + "ORDER BY o.createdAt DESC")
    List<Order> findAllForAdmin(@Param("status") OrderStatus status, @Param("businessId") Long businessId);
}
