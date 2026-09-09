package com.foody.products.controller;

import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.service.MenuService;
import com.foody.products.dto.ProductResponse;
import com.foody.products.entity.Product;
import com.foody.products.service.ProductService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 0 exposes read-only product lookups, mirroring BusinessController/MenuController.
 * Product management (create, update price/availability) arrives with the business
 * owner panel in Phase 1.
 */
@RestController
public class ProductController {

    private final ProductService productService;
    private final MenuService menuService;
    private final BusinessService businessService;

    public ProductController(ProductService productService, MenuService menuService,
                             BusinessService businessService) {
        this.productService = productService;
        this.menuService = menuService;
        this.businessService = businessService;
    }

    @GetMapping("/api/products/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        requirePublicMenu(product.getMenuId());
        return ProductResponse.from(product);
    }

    @GetMapping("/api/menus/{menuId}/products")
    public List<ProductResponse> getProductsForMenu(@PathVariable Long menuId) {
        requirePublicMenu(menuId);

        return productService.findByMenuId(menuId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    private void requirePublicMenu(Long menuId) {
        var menu = menuService.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuId));
        businessService.findByIdAndStatus(menu.getBusinessId(), BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuId));
    }
}
