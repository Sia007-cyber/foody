package com.foody.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(

        @NotNull(message = "menuId is required")
        Long menuId,

        @NotBlank(message = "name is required")
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "price cannot be negative")
        BigDecimal price,

        @Size(max = 512)
        String imageUrl,

        Integer displayOrder
) {
}
