package com.foody.products.service;

import com.foody.products.dto.CreateProductRequest;
import com.foody.products.dto.UpdateProductRequest;
import com.foody.products.entity.Product;
import java.util.List;
import java.util.Optional;

/**
 * Public contract for the products module. Other modules depend on this interface only.
 */
public interface ProductService {

    Optional<Product> findById(Long id);

    List<Product> findByMenuId(Long menuId);

    // Business panel: creates/updates a product on a menu that belongs to the
    // calling owner's own business.
    Product createProduct(Long ownerUserId, CreateProductRequest request);

    Product updateProduct(Long ownerUserId, Long productId, UpdateProductRequest request);

    // Business panel: deletes a product from a menu owned by the calling owner.
    void deleteProduct(Long ownerUserId, Long productId);
}
