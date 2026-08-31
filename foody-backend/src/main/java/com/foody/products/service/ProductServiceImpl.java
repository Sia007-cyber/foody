package com.foody.products.service;

import com.foody.products.entity.Product;
import com.foody.products.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByMenuId(Long menuId) {
        return productRepository.findByMenuIdOrderByDisplayOrderAsc(menuId);
    }
}
