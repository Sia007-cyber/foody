package com.foody.reviews.service;
import com.foody.reviews.dto.*;
public interface ProductReviewService {
 ProductReviewListResponse list(Long productId); ProductReviewResponse mine(Long productId,Long reviewerId);
 ProductReviewResponse create(Long productId,Long reviewerId,ReviewRequest request); ProductReviewResponse update(Long productId,Long reviewerId,ReviewRequest request);
 void delete(Long productId,Long reviewerId); OwnerReviewsResponse ownerReviews(Long ownerId);
}
