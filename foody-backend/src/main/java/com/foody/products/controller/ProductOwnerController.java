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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.foody.common.storage.ImageUploadService;

/** Business panel — create/update products on the calling owner's own menus. */
@RestController
@RequestMapping("/api/business/products")
@PreAuthorize("hasRole('BUSINESS_OWNER')")
public class ProductOwnerController {

    private final ProductService productService;
    private final ImageUploadService imageUploadService;

    public ProductOwnerController(ProductService productService, ImageUploadService imageUploadService) {
        this.productService = productService;
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse replaceProductImage(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                               @PathVariable Long id,
                                               @RequestParam("file") MultipartFile file) {
        var upload = imageUploadService.store(ImageUploadService.UploadCategory.PRODUCT, file);
        try {
            var replacement = productService.replaceProductImage(principal.getUserId(), id, upload.publicUrl());
            try { imageUploadService.deleteManagedUrl(replacement.previousUrl()); }
            catch (RuntimeException ignored) { /* committed replacement remains valid */ }
            return ProductResponse.from(replacement.value());
        } catch (RuntimeException ex) {
            try { imageUploadService.deleteManagedUrl(upload.publicUrl()); }
            catch (RuntimeException cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }

    @org.springframework.web.bind.annotation.GetMapping
    public java.util.List<ProductResponse> listProducts(@AuthenticationPrincipal FoodyUserPrincipal principal,
            @org.springframework.web.bind.annotation.RequestParam Long menuId) {
        return productService.findMyProducts(principal.getUserId(), menuId).stream().map(ProductResponse::from).toList();
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
        String deletedImageUrl = productService.deleteProduct(principal.getUserId(), id);
        try { imageUploadService.deleteManagedUrl(deletedImageUrl); }
        catch (RuntimeException ignored) { /* the database deletion has committed */ }
        return ResponseEntity.noContent().build();
    }
}
