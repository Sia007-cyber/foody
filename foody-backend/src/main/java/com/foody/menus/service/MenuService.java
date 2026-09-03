package com.foody.menus.service;

import com.foody.menus.dto.CreateMenuRequest;
import com.foody.menus.dto.UpdateMenuRequest;
import com.foody.menus.entity.Menu;
import java.util.List;
import java.util.Optional;

/**
 * Public contract for the menus module. Other modules depend on this interface only.
 */
public interface MenuService {

    List<Menu> findByBusinessId(Long businessId);

    Optional<Menu> findById(Long id);

    // Business panel: lists menus under the calling owner's own business, regardless
    // of the business's approval status (unlike findByBusinessId, used by the public API).
    List<Menu> findMyMenus(Long ownerUserId);

    // Business panel: creates a menu under the calling owner's own business.
    Menu createMenu(Long ownerUserId, CreateMenuRequest request);

    // Business panel: renames a menu owned by the calling owner.
    Menu updateMenu(Long ownerUserId, Long menuId, UpdateMenuRequest request);

    // Business panel: deletes a menu (and its products, via DB cascade) owned by the calling owner.
    void deleteMenu(Long ownerUserId, Long menuId);
}
