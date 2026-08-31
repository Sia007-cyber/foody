package com.foody.products.dto;

import com.foody.products.entity.Product;
import java.math.BigDecimal;

/** Public view of a product. Returned instead of the entity. */
public record ProductResponse(
        Long id,
        Long menuId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Boolean isAvailable,
        Integer displayOrder) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getMenuId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getImageUrl(), p.getIsAvailable(), p.getDisplayOrder());
    }
}
