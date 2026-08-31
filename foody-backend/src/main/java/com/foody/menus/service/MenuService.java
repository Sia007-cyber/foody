package com.foody.menus.service;

import com.foody.menus.dto.CreateMenuRequest;
import com.foody.menus.entity.Menu;
import java.util.List;
import java.util.Optional;

/**
 * Public contract for the menus module. Other modules depend on this interface only.
 */
public interface MenuService {

    List<Menu> findByBusinessId(Long businessId);

    Optional<Menu> findById(Long id);

    // Business panel: creates a menu under the calling owner's own business.
    Menu createMenu(Long ownerUserId, CreateMenuRequest request);
}
