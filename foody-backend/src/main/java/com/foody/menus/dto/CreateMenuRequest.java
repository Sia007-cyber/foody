package com.foody.menus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMenuRequest(

        @NotBlank(message = "name is required")
        @Size(max = 255)
        String name,

        Integer displayOrder
) {
}
