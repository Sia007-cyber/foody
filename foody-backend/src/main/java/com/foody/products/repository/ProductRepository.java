package com.foody.products.repository;

import com.foody.products.entity.Product;
import com.foody.products.dto.ProductDiscoveryResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByMenuIdOrderByDisplayOrderAsc(Long menuId);

    @Query("select p from Product p, Menu m where p.menuId=m.id and m.businessId=:businessId and p.id in :ids and p.isAvailable=true")
    List<Product> findAvailableForBusiness(@Param("businessId") Long businessId, @Param("ids") List<Long> ids);

    @Query("""
            select new com.foody.products.dto.ProductDiscoveryResponse(
                p.id, p.menuId, b.id, b.name, p.name, p.price, p.imageUrl,
                cast(avg(r.rating) as double), count(r))
            from Product p
            join Menu m on m.id = p.menuId
            join Business b on b.id = m.businessId
            left join ProductReview r on r.productId = p.id
                and r.moderationStatus = com.foody.reviews.entity.ReviewModerationStatus.APPROVED
            where p.isAvailable = true
              and b.status = com.foody.businesses.entity.BusinessStatus.APPROVED
            group by p.id, p.menuId, b.id, b.name, p.name, p.price, p.imageUrl
            having count(r) > 0
            order by avg(r.rating) desc, count(r) desc, p.name asc, p.id asc
            """)
    List<ProductDiscoveryResponse> findTopRatedPublic(Pageable pageable);

    @Query("""
            select new com.foody.products.dto.ProductDiscoveryResponse(
                p.id, p.menuId, b.id, b.name, p.name, p.price, p.imageUrl,
                cast(avg(r.rating) as double), count(r))
            from Product p
            join Menu m on m.id = p.menuId
            join Business b on b.id = m.businessId
            left join ProductReview r on r.productId = p.id
                and r.moderationStatus = com.foody.reviews.entity.ReviewModerationStatus.APPROVED
            where p.isAvailable = true
              and b.status = com.foody.businesses.entity.BusinessStatus.APPROVED
              and replace(replace(lower(p.name), 'ي', 'ی'), 'ك', 'ک') like concat('%', :query, '%')
            group by p.id, p.menuId, b.id, b.name, p.name, p.price, p.imageUrl
            order by case when replace(replace(lower(p.name), 'ي', 'ی'), 'ك', 'ک') = :query then 0 else 1 end,
                     p.name asc, p.id asc
            """)
    List<ProductDiscoveryResponse> searchPublic(@Param("query") String query, Pageable pageable);
}
