package com.foody.businesses.service;

import com.foody.businesses.dto.CreateBusinessRequest;
import com.foody.businesses.dto.UpdateBusinessProfileRequest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import java.util.List;
import java.util.Optional;

/**
 * Public contract for the businesses module. Other modules depend on this interface only.
 */
public interface BusinessService {

    Optional<Business> findById(Long id);

    Optional<Business> findByIdAndStatus(Long id, com.foody.businesses.entity.BusinessStatus status);

    Optional<Business> findByOwnerUserId(Long ownerUserId);

    // Owner onboarding (POST /api/business): one-time self-registration. Throws
    // DuplicateResourceException if this owner already has a business.
    Business createForOwner(Long ownerUserId, CreateBusinessRequest request);

    // Phase 1 Discover (GET /api/businesses?type=&search=): only APPROVED businesses,
    // optionally filtered by business type code and/or a case-insensitive name search.
    // Pass null/blank for either param to skip that filter.
    List<Business> search(String type, String search);

    Business updateProfile(Long ownerUserId, UpdateBusinessProfileRequest request);

    // Admin panel: list businesses by approval status. Pass null for all statuses.
    List<Business> findAll(BusinessStatus statusFilter);

    // Admin panel: PENDING -> APPROVED/REJECTED, APPROVED -> SUSPENDED.
    // Throws InvalidStateTransitionException for any other transition.
    Business updateStatus(Long businessId, BusinessStatus newStatus);

    // Admin dashboard summary.
    long countByStatus(BusinessStatus status);
}
