package com.foody.reviews.service;

import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.entity.Business;
import com.foody.businesses.service.BusinessService;
import com.foody.businesses.service.CustomerBusinessAccessPolicy;
import com.foody.common.exception.DuplicateResourceException;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.reviews.dto.*;
import com.foody.reviews.entity.Review;
import com.foody.reviews.repository.*;
import com.foody.users.entity.User;
import com.foody.users.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReviewServiceImpl implements ReviewService {
    private static final int MAX_COMMENT_LENGTH = 2000;
    private final ReviewRepository reviews;
    private final BusinessService businesses;
    private final UserService users;

    ReviewServiceImpl(ReviewRepository reviews, BusinessService businesses, UserService users) {
        this.reviews = reviews;
        this.businesses = businesses;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewListResponse list(Long businessId) {
        requireApprovedBusiness(businessId);
        List<Review> found = reviews.findByBusinessIdOrderByCreatedAtDescIdDesc(businessId);
        Map<Long, User> reviewers = users.findAllById(found.stream().map(Review::getCustomerUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<ReviewResponse> responses = found.stream()
                .map(review -> ReviewResponse.from(review, reviewer(reviewers, review).getFullName()))
                .toList();
        ReviewRatingSummary summary = reviews.summarize(businessId);
        BigDecimal average = summary.getAverageRating() == null
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(summary.getAverageRating()).setScale(2, RoundingMode.HALF_UP);
        return new ReviewListResponse(responses, average, summary.getReviewCount());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse mine(Long businessId, Long customerUserId) {
        requireCustomerTarget(businessId, customerUserId);
        return response(ownedReview(businessId, customerUserId));
    }

    @Override
    @Transactional
    public ReviewResponse create(Long businessId, Long customerUserId, ReviewRequest request) {
        requireCustomerTarget(businessId, customerUserId);
        validate(request);
        if (reviews.existsByBusinessIdAndCustomerUserId(businessId, customerUserId)) {
            throw duplicate();
        }
        Review review = new Review();
        review.setBusinessId(businessId);
        review.setCustomerUserId(customerUserId);
        apply(review, request);
        try {
            return response(reviews.saveAndFlush(review));
        } catch (DataIntegrityViolationException ex) {
            throw duplicate();
        }
    }

    @Override
    @Transactional
    public ReviewResponse update(Long businessId, Long customerUserId, ReviewRequest request) {
        requireCustomerTarget(businessId, customerUserId);
        validate(request);
        Review review = ownedReview(businessId, customerUserId);
        apply(review, request);
        return response(reviews.saveAndFlush(review));
    }

    @Override
    @Transactional
    public void delete(Long businessId, Long customerUserId) {
        requireCustomerTarget(businessId, customerUserId);
        reviews.delete(ownedReview(businessId, customerUserId));
    }

    @Override
    @Transactional
    public void deleteForAdmin(Long reviewId) {
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        reviews.delete(review);
    }

    private void requireApprovedBusiness(Long businessId) {
        businesses.findByIdAndStatus(businessId, BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
    }

    private void requireCustomerTarget(Long businessId, Long customerUserId) {
        Business business = businesses.findByIdAndStatus(businessId, BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
        CustomerBusinessAccessPolicy.requireNotOwnedBy(customerUserId, business);
    }

    private Review ownedReview(Long businessId, Long customerUserId) {
        return reviews.findByBusinessIdAndCustomerUserId(businessId, customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for this customer and business"));
    }

    private ReviewResponse response(Review review) {
        User reviewer = users.findById(review.getCustomerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Review customer not found"));
        return ReviewResponse.from(review, reviewer.getFullName());
    }

    private static User reviewer(Map<Long, User> users, Review review) {
        User user = users.get(review.getCustomerUserId());
        if (user == null) throw new ResourceNotFoundException("Review customer not found");
        return user;
    }

    private static void validate(ReviewRequest request) {
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new InvalidRequestException("Rating must be an integer from 1 to 5");
        }
        if (request.comment() != null && request.comment().trim().length() > MAX_COMMENT_LENGTH) {
            throw new InvalidRequestException("Comment must not exceed " + MAX_COMMENT_LENGTH + " characters");
        }
    }

    private static void apply(Review review, ReviewRequest request) {
        review.setRating(request.rating());
        String comment = request.comment();
        review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
    }

    private static DuplicateResourceException duplicate() {
        return new DuplicateResourceException("You have already reviewed this business; update your existing review instead");
    }
}
