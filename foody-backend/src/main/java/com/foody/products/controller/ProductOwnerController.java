package com.foody.products.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.products.dto.CreateProductRequest;
import com.foody.products.dto.ProductResponse;
import com.foody.products.dto.UpdateProductRequest;
import com.foody.products.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Business panel — create/update products on the calling owner's own menus. */
@RestController
@RequestMapping("/api/business/products")
@PreAuthorize("hasRole('BUSINESS_OWNER')")
public class ProductOwnerController {

    private final ProductService productService;

    public ProductOwnerController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponse createProduct(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                         @Valid @RequestBody CreateProductRequest request) {
        return ProductResponse.from(productService.createProduct(principal.getUserId(), request));
    }

    @PatchMapping("/{id}")
    public ProductResponse updateProduct(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                         @PathVariable Long id,
                                         @Valid @RequestBody UpdateProductRequest request) {
        return ProductResponse.from(productService.updateProduct(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                              @PathVariable Long id) {
        productService.deleteProduct(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
