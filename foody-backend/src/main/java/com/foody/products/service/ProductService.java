package com.foody.products.service;

import com.foody.products.entity.Product;
import java.util.List;
import java.util.Optional;

/**
 * Public contract for the products module. Other modules depend on this interface only.
 */
public interface ProductService {

    Optional<Product> findById(Long id);

    List<Product> findByMenuId(Long menuId);
}
