package com.foody.menus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PATCH /api/business/menus/{id} — currently only the name is editable. */
public record UpdateMenuRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255)
        String name
) {
}
