package com.foody.menus.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.menus.dto.CreateMenuRequest;
import com.foody.menus.dto.MenuResponse;
import com.foody.menus.dto.UpdateMenuRequest;
import com.foody.menus.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Business panel — create/rename/delete a menu under the calling owner's own business. */
@RestController
@RequestMapping("/api/business/menus")
@PreAuthorize("hasRole('BUSINESS_OWNER')")
public class MenuOwnerController {

    private final MenuService menuService;

    public MenuOwnerController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    public MenuResponse createMenu(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                   @Valid @RequestBody CreateMenuRequest request) {
        return MenuResponse.from(menuService.createMenu(principal.getUserId(), request));
    }

    @PatchMapping("/{id}")
    public MenuResponse updateMenu(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateMenuRequest request) {
        return MenuResponse.from(menuService.updateMenu(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                           @PathVariable Long id) {
        menuService.deleteMenu(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
