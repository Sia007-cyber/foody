package com.foody.businesses.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
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
}
