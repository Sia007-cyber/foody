package com.foody.products.repository;

import com.foody.products.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByMenuIdOrderByDisplayOrderAsc(Long menuId);

    @Query("select p from Product p, Menu m where p.menuId=m.id and m.businessId=:businessId and p.id in :ids and p.isAvailable=true")
    List<Product> findAvailableForBusiness(@Param("businessId") Long businessId, @Param("ids") List<Long> ids);
}
