package com.foody.favorites.service;

import com.foody.businesses.entity.Business;
import java.util.List;

public interface FavoriteService {
    void add(Long customerUserId, Long businessId);
    void remove(Long customerUserId, Long businessId);
    List<Business> listPublicBusinesses(Long customerUserId);
    List<Long> listBusinessIds(Long customerUserId);
}
