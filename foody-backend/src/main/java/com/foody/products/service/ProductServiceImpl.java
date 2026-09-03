package com.foody.products.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
import com.foody.products.dto.CreateProductRequest;
import com.foody.products.dto.UpdateProductRequest;
import com.foody.products.entity.Product;
import com.foody.products.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MenuService menuService;
    private final BusinessService businessService;

    ProductServiceImpl(ProductRepository productRepository, MenuService menuService,
                       BusinessService businessService) {
        this.productRepository = productRepository;
        this.menuService = menuService;
        this.businessService = businessService;
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

    @Override
    @Transactional
    public Product createProduct(Long ownerUserId, CreateProductRequest request) {
        Menu menu = requireOwnedMenu(ownerUserId, request.menuId());

        Product product = new Product();
        product.setMenuId(menu.getId());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImageUrl(request.imageUrl());
        product.setIsAvailable(true);
        product.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long ownerUserId, Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        // Verifies the product's menu belongs to the calling owner's business;
        // throws if not, so this also acts as the ownership check.
        requireOwnedMenu(ownerUserId, product.getMenuId());

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.isAvailable() != null) product.setIsAvailable(request.isAvailable());
        if (request.displayOrder() != null) product.setDisplayOrder(request.displayOrder());

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long ownerUserId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        // Ownership check, same pattern as updateProduct.
        requireOwnedMenu(ownerUserId, product.getMenuId());

        productRepository.delete(product);
    }

    private Menu requireOwnedMenu(Long ownerUserId, Long menuId) {
        Business business = businessService.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No business found for this owner"));

        Menu menu = menuService.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuId));

        if (!menu.getBusinessId().equals(business.getId())) {
            throw new InvalidRequestException("Menu " + menuId + " does not belong to your business");
        }
        return menu;
    }
}
