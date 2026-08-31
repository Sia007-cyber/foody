package com.foody.menus.dto;

import com.foody.menus.entity.Menu;

/** Public view of a menu. Returned instead of the entity. */
public record MenuResponse(
        Long id,
        Long businessId,
        String name,
        Integer displayOrder) {

    public static MenuResponse from(Menu m) {
        return new MenuResponse(m.getId(), m.getBusinessId(), m.getName(), m.getDisplayOrder());
    }
}
