package com.foody.menus.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.dto.CreateMenuRequest;
import com.foody.menus.dto.UpdateMenuRequest;
import com.foody.menus.entity.Menu;
import com.foody.menus.repository.MenuRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final BusinessService businessService;

    MenuServiceImpl(MenuRepository menuRepository, BusinessService businessService) {
        this.menuRepository = menuRepository;
        this.businessService = businessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> findByBusinessId(Long businessId) {
        return menuRepository.findByBusinessIdOrderByDisplayOrderAsc(businessId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Menu> findById(Long id) {
        return menuRepository.findById(id);
    }

    @Override
    @Transactional
    public Menu createMenu(Long ownerUserId, CreateMenuRequest request) {
        Business business = businessService.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No business found for this owner"));

        Menu menu = new Menu();
        menu.setBusinessId(business.getId());
        menu.setName(request.name());
        menu.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        return menuRepository.save(menu);
    }

    @Override
    @Transactional
    public Menu updateMenu(Long ownerUserId, Long menuId, UpdateMenuRequest request) {
        Menu menu = requireOwnedMenu(ownerUserId, menuId);
        menu.setName(request.name());
        return menuRepository.save(menu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long ownerUserId, Long menuId) {
        Menu menu = requireOwnedMenu(ownerUserId, menuId);
        // Products under this menu cascade-delete at the DB level (fk_products_menu ON DELETE CASCADE).
        menuRepository.delete(menu);
    }

    private Menu requireOwnedMenu(Long ownerUserId, Long menuId) {
        Business business = businessService.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No business found for this owner"));

        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuId));

        if (!menu.getBusinessId().equals(business.getId())) {
            throw new InvalidRequestException("Menu " + menuId + " does not belong to your business");
        }
        return menu;
    }
}
