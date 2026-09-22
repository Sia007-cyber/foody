package com.foody.reviews.service;

import com.foody.businesses.repository.BusinessRepository;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.repository.MenuRepository;
import com.foody.products.repository.ProductRepository;
import com.foody.reviews.dto.AdminReviewResponse;
import com.foody.reviews.entity.*;
import com.foody.reviews.repository.*;
import com.foody.users.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewModerationService {
    private final ReviewRepository reviews; private final ProductReviewRepository productReviews;
    private final UserRepository users; private final BusinessRepository businesses;
    private final ProductRepository products; private final MenuRepository menus;
    public ReviewModerationService(ReviewRepository reviews,ProductReviewRepository productReviews,UserRepository users,BusinessRepository businesses,ProductRepository products,MenuRepository menus){this.reviews=reviews;this.productReviews=productReviews;this.users=users;this.businesses=businesses;this.products=products;this.menus=menus;}

    @Transactional(readOnly=true)
    public List<AdminReviewResponse> list(ReviewModerationStatus status){
        List<AdminReviewResponse> result=new ArrayList<>();
        reviews.findByModerationStatusOrderByCreatedAtDescIdDesc(status).forEach(r->{var user=users.findById(r.getCustomerUserId()).orElseThrow();var business=businesses.findById(r.getBusinessId()).orElseThrow();result.add(new AdminReviewResponse(r.getId(),"BUSINESS",user.getFullName(),business.getName(),business.getName(),r.getRating(),r.getComment(),r.getModerationStatus(),r.getCreatedAt()));});
        productReviews.findByModerationStatusOrderByCreatedAtDescIdDesc(status).forEach(r->{var user=users.findById(r.getReviewerUserId()).orElseThrow();var product=products.findById(r.getProductId()).orElseThrow();var menu=menus.findById(product.getMenuId()).orElseThrow();var business=businesses.findById(menu.getBusinessId()).orElseThrow();result.add(new AdminReviewResponse(r.getId(),"PRODUCT",user.getFullName(),business.getName(),product.getName(),r.getRating(),r.getComment(),r.getModerationStatus(),r.getCreatedAt()));});
        return result.stream().sorted(Comparator.comparing(AdminReviewResponse::createdAt).reversed().thenComparing(AdminReviewResponse::id,Comparator.reverseOrder())).toList();
    }

    @Transactional public AdminReviewResponse moderate(String type,Long id,ReviewModerationStatus status){
        if(status==ReviewModerationStatus.PENDING)throw new IllegalArgumentException("Moderation decision must be APPROVED or REJECTED");
        if("BUSINESS".equals(type)){Review r=reviews.findById(id).orElseThrow(()->new ResourceNotFoundException("Review not found: "+id));r.setModerationStatus(status);reviews.saveAndFlush(r);return businessResponse(r);}
        if("PRODUCT".equals(type)){ProductReview r=productReviews.findById(id).orElseThrow(()->new ResourceNotFoundException("Product review not found: "+id));r.setModerationStatus(status);productReviews.saveAndFlush(r);return productResponse(r);}
        throw new ResourceNotFoundException("Review type not found: "+type);
    }
    private AdminReviewResponse businessResponse(Review r){var user=users.findById(r.getCustomerUserId()).orElseThrow();var business=businesses.findById(r.getBusinessId()).orElseThrow();return new AdminReviewResponse(r.getId(),"BUSINESS",user.getFullName(),business.getName(),business.getName(),r.getRating(),r.getComment(),r.getModerationStatus(),r.getCreatedAt());}
    private AdminReviewResponse productResponse(ProductReview r){var user=users.findById(r.getReviewerUserId()).orElseThrow();var product=products.findById(r.getProductId()).orElseThrow();var menu=menus.findById(product.getMenuId()).orElseThrow();var business=businesses.findById(menu.getBusinessId()).orElseThrow();return new AdminReviewResponse(r.getId(),"PRODUCT",user.getFullName(),business.getName(),product.getName(),r.getRating(),r.getComment(),r.getModerationStatus(),r.getCreatedAt());}
}
