package com.foody.orders.repository;

import com.foody.orders.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndCustomerUserId(Long id, Long customerUserId);

    List<Order> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);
}
