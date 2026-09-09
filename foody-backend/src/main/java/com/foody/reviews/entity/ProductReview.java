package com.foody.reviews.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "product_reviews", uniqueConstraints = @UniqueConstraint(name = "uk_product_reviews_product_reviewer", columnNames = {"product_id", "reviewer_user_id"}))
public class ProductReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "reviewer_user_id", nullable = false) private Long reviewerUserId;
    @Column(nullable = false) private Integer rating;
    @Column(length = 2000) private String comment;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate(){Instant now=Instant.now().truncatedTo(ChronoUnit.MICROS);createdAt=now;updatedAt=now;}
    @PreUpdate void onUpdate(){updatedAt=Instant.now().truncatedTo(ChronoUnit.MICROS);}
    public Long getId(){return id;} public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;}
    public Long getReviewerUserId(){return reviewerUserId;} public void setReviewerUserId(Long v){reviewerUserId=v;}
    public Integer getRating(){return rating;} public void setRating(Integer v){rating=v;} public String getComment(){return comment;} public void setComment(String v){comment=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
