package com.foody.products.repository;

import com.foody.products.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByMenuIdOrderByDisplayOrderAsc(Long menuId);
}
