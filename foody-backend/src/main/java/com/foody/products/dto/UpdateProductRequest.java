package com.foody.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** PATCH /api/business/products/{id} — only non-null fields are applied. */
public record UpdateProductRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @DecimalMin(value = "0.0", inclusive = true, message = "price cannot be negative") BigDecimal price,
        @Size(max = 512) String imageUrl,
        Boolean isAvailable,
        Integer displayOrder) {
}
