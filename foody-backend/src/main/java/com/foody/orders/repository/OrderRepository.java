package com.foody.orders.repository;

import com.foody.orders.entity.Order;
import com.foody.orders.entity.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndCustomerUserId(Long id, Long customerUserId);

    List<Order> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);

    Optional<Order> findByIdAndBusinessId(Long id, Long businessId);

    List<Order> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    List<Order> findByBusinessIdAndStatusOrderByCreatedAtDesc(Long businessId, OrderStatus status);
}
