package com.foody.businesses.repository;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByIdAndStatus(Long id, com.foody.businesses.entity.BusinessStatus status);

    // Phase 1 assumes one business per owner for the business panel; multi-business
    // ownership (if ever needed) would replace this with a List-returning finder.
    Optional<Business> findByOwnerUserId(Long ownerUserId);

    // Admin panel: businesses awaiting/holding a given approval status.
    List<Business> findByStatusOrderByCreatedAtDesc(BusinessStatus status);

    // Admin dashboard summary.
    long countByStatus(BusinessStatus status);
}
