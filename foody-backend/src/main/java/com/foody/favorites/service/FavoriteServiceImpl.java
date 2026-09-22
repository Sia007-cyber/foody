package com.foody.favorites.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.favorites.repository.BusinessFavoriteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class FavoriteServiceImpl implements FavoriteService {
    private final BusinessFavoriteRepository favorites;
    private final BusinessRepository businesses;
    FavoriteServiceImpl(BusinessFavoriteRepository favorites, BusinessRepository businesses) { this.favorites = favorites; this.businesses = businesses; }

    @Override @Transactional
    public void add(Long customerUserId, Long businessId) {
        requirePublic(businessId);
        favorites.addIfAbsent(customerUserId, businessId);
    }

    @Override @Transactional
    public void remove(Long customerUserId, Long businessId) { favorites.deleteByCustomerUserIdAndBusinessId(customerUserId, businessId); }

    @Override @Transactional(readOnly = true)
    public List<Business> listPublicBusinesses(Long customerUserId) {
        return businesses.findPublicFavoritesByCustomerUserId(customerUserId, BusinessStatus.APPROVED);
    }

    @Override @Transactional(readOnly = true)
    public List<Long> listBusinessIds(Long customerUserId) {
        return listPublicBusinesses(customerUserId).stream().map(Business::getId).toList();
    }

    private void requirePublic(Long businessId) {
        businesses.findByIdAndStatus(businessId, BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Public business not found: " + businessId));
    }
}
