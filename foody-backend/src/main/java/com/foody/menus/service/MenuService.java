package com.foody.menus.service;

import com.foody.menus.entity.Menu;
import java.util.List;
import java.util.Optional;

/**
 * Public contract for the menus module. Other modules depend on this interface only.
 */
public interface MenuService {

    List<Menu> findByBusinessId(Long businessId);

    Optional<Menu> findById(Long id);
}
