package com.foody.businesses.service;

import com.foody.businesses.dto.UpdateBusinessProfileRequest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;

    BusinessServiceImpl(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Business> findById(Long id) {
        return businessRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Business> findByIdAndStatus(Long id, BusinessStatus status) {
        return businessRepository.findByIdAndStatus(id, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Business> findByOwnerUserId(Long ownerUserId) {
        return businessRepository.findByOwnerUserId(ownerUserId);
    }

    @Override
    @Transactional
    public Business updateProfile(Long ownerUserId, UpdateBusinessProfileRequest request) {
        Business business = businessRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No business found for this owner"));

        if (request.name() != null) business.setName(request.name());
        if (request.description() != null) business.setDescription(request.description());
        if (request.address() != null) business.setAddress(request.address());
        if (request.latitude() != null) business.setLatitude(request.latitude());
        if (request.longitude() != null) business.setLongitude(request.longitude());
        if (request.phone() != null) business.setPhone(request.phone());
        if (request.coverImageUrl() != null) business.setCoverImageUrl(request.coverImageUrl());

        return businessRepository.save(business);
    }
}
