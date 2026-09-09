package com.foody.reviews.controller;
import com.foody.auth.security.FoodyUserPrincipal; import com.foody.reviews.dto.*; import com.foody.reviews.service.ProductReviewService; import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {private final ProductReviewService reviews;public ProductReviewController(ProductReviewService r){reviews=r;}
 @GetMapping public ProductReviewListResponse list(@PathVariable Long productId){return reviews.list(productId);}
 @GetMapping("/mine") @PreAuthorize("hasAnyRole('CUSTOMER','BUSINESS_OWNER')") public ProductReviewResponse mine(@PathVariable Long productId,@AuthenticationPrincipal FoodyUserPrincipal p){return reviews.mine(productId,p.getUserId());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('CUSTOMER','BUSINESS_OWNER')") public ProductReviewResponse create(@PathVariable Long productId,@AuthenticationPrincipal FoodyUserPrincipal p,@Valid @RequestBody ReviewRequest r){return reviews.create(productId,p.getUserId(),r);}
 @PatchMapping("/mine") @PreAuthorize("hasAnyRole('CUSTOMER','BUSINESS_OWNER')") public ProductReviewResponse update(@PathVariable Long productId,@AuthenticationPrincipal FoodyUserPrincipal p,@Valid @RequestBody ReviewRequest r){return reviews.update(productId,p.getUserId(),r);}
 @DeleteMapping("/mine") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('CUSTOMER','BUSINESS_OWNER')") public void delete(@PathVariable Long productId,@AuthenticationPrincipal FoodyUserPrincipal p){reviews.delete(productId,p.getUserId());}
}
