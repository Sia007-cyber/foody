package com.foody.menus.controller;

import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.dto.MenuResponse;
import com.foody.menus.service.MenuService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 0 exposes a read-only menu listing per business, mirroring the read-only
 * pattern established by BusinessController. Menu/product management (create,
 * update, availability) arrives with the business owner panel in Phase 1.
 */
@RestController
@RequestMapping("/api/businesses/{businessId}/menus")
public class MenuController {

    private final MenuService menuService;
    private final BusinessService businessService;

    public MenuController(MenuService menuService, BusinessService businessService) {
        this.menuService = menuService;
        this.businessService = businessService;
    }

    @GetMapping
    public List<MenuResponse> getMenusForBusiness(@PathVariable Long businessId) {
        businessService.findByIdAndStatus(businessId, BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));

        return menuService.findByBusinessId(businessId).stream()
                .map(MenuResponse::from)
                .toList();
    }
}
