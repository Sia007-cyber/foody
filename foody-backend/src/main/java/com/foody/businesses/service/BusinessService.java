package com.foody.businesses.service;

import com.foody.businesses.dto.UpdateBusinessProfileRequest;
import com.foody.businesses.entity.Business;
import java.util.Optional;

/**
 * Public contract for the businesses module. Other modules depend on this interface only.
 */
public interface BusinessService {

    Optional<Business> findById(Long id);

    Optional<Business> findByIdAndStatus(Long id, com.foody.businesses.entity.BusinessStatus status);

    Optional<Business> findByOwnerUserId(Long ownerUserId);

    Business updateProfile(Long ownerUserId, UpdateBusinessProfileRequest request);
}
