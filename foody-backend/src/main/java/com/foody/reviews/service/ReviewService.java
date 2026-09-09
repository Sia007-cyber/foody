package com.foody.reviews.service;

import com.foody.reviews.dto.*;

public interface ReviewService {
    ReviewListResponse list(Long businessId);
    ReviewResponse mine(Long businessId, Long customerUserId);
    ReviewResponse create(Long businessId, Long customerUserId, ReviewRequest request);
    ReviewResponse update(Long businessId, Long customerUserId, ReviewRequest request);
    void delete(Long businessId, Long customerUserId);
    void deleteForAdmin(Long reviewId);
}
