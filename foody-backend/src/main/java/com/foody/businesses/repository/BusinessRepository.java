package com.foody.businesses.repository;

import com.foody.businesses.entity.Business;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByIdAndStatus(Long id, com.foody.businesses.entity.BusinessStatus status);

    // Phase 1 assumes one business per owner for the business panel; multi-business
    // ownership (if ever needed) would replace this with a List-returning finder.
    Optional<Business> findByOwnerUserId(Long ownerUserId);
}
