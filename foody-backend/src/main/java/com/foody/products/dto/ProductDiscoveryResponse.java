package com.foody.products.dto;

import java.math.BigDecimal;

/** Public product card with its approved business and approved-review aggregate. */
public record ProductDiscoveryResponse(
        Long id,
        Long menuId,
        Long businessId,
        String businessName,
        String name,
        BigDecimal price,
        String imageUrl,
        Double averageRating,
        Long reviewCount) {
}
