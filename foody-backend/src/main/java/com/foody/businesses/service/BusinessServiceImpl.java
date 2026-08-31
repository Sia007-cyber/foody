package com.foody.businesses.service;

import com.foody.businesses.dto.UpdateBusinessProfileRequest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BusinessServiceImpl implements BusinessService {

    // Admin-driven approval transitions:
    // PENDING -> APPROVED -> SUSPENDED
    // PENDING -> REJECTED
    private static final Map<BusinessStatus, Set<BusinessStatus>> VALID_ADMIN_TRANSITIONS = Map.of(
            BusinessStatus.PENDING, Set.of(BusinessStatus.APPROVED, BusinessStatus.REJECTED),
            BusinessStatus.APPROVED, Set.of(BusinessStatus.SUSPENDED)
    );

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
    @Transactional(readOnly = true)
    public List<Business> search(String type, String search) {
        String normalizedType = blankToNull(type);
        String normalizedSearch = blankToNull(search);
        return businessRepository.search(BusinessStatus.APPROVED, normalizedType, normalizedSearch);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
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

    @Override
    @Transactional(readOnly = true)
    public List<Business> findAll(BusinessStatus statusFilter) {
        if (statusFilter != null) {
            return businessRepository.findByStatusOrderByCreatedAtDesc(statusFilter);
        }
        return businessRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional
    public Business updateStatus(Long businessId, BusinessStatus newStatus) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));

        Set<BusinessStatus> allowedNext = VALID_ADMIN_TRANSITIONS.getOrDefault(business.getStatus(), Set.of());
        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot move business " + businessId + " from " + business.getStatus() + " to " + newStatus);
        }
        business.setStatus(newStatus);
        return businessRepository.save(business);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(BusinessStatus status) {
        return businessRepository.countByStatus(status);
    }
}
