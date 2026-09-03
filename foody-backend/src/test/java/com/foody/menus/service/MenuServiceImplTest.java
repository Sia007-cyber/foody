package com.foody.menus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.businesses.entity.Business;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.DuplicateResourceException;
import com.foody.common.exception.InvalidRequestException;
import com.foody.menus.dto.CreateMenuRequest;
import com.foody.menus.dto.UpdateMenuRequest;
import com.foody.menus.entity.Menu;
import com.foody.menus.repository.MenuRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock MenuRepository menuRepository;
    @Mock BusinessService businessService;

    MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl(menuRepository, businessService);
    }

    @Test
    void createMenu_savesUnderOwnersBusiness() {
        Business business = new Business();
        business.setId(10L);

        when(businessService.findByOwnerUserId(1L)).thenReturn(Optional.of(business));
        when(menuRepository.save(org.mockito.ArgumentMatchers.any(Menu.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Menu result = menuService.createMenu(1L, new CreateMenuRequest("Drinks", null));

        assertThat(result.getBusinessId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Drinks");
        assertThat(result.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    void createMenu_rejectsSecondMenuForSameBusiness() {
        Business business = new Business();
        business.setId(10L);
        Menu existingMenu = new Menu();
        existingMenu.setId(1L);
        existingMenu.setBusinessId(10L);

        when(businessService.findByOwnerUserId(1L)).thenReturn(Optional.of(business));
        when(menuRepository.findByBusinessIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(existingMenu));

        assertThatThrownBy(() -> menuService.createMenu(1L, new CreateMenuRequest("Drinks", null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(menuRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findMyMenus_returnsOwnersMenus() {
        Business business = new Business();
        business.setId(10L);
        Menu menu = new Menu();
        menu.setId(1L);
        menu.setBusinessId(10L);
        menu.setName("Breakfast");

        when(businessService.findByOwnerUserId(1L)).thenReturn(Optional.of(business));
        when(menuRepository.findByBusinessIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(menu));

        List<Menu> result = menuService.findMyMenus(1L);

        assertThat(result).containsExactly(menu);
    }

    @Test
    void findByBusinessId_returnsMenusOrderedByDisplayOrder() {
        Menu menu1 = new Menu();
        menu1.setId(1L);
        menu1.setBusinessId(10L);
        menu1.setName("Breakfast");
        menu1.setDisplayOrder(0);

        Menu menu2 = new Menu();
        menu2.setId(2L);
        menu2.setBusinessId(10L);
        menu2.setName("Drinks");
        menu2.setDisplayOrder(1);

        when(menuRepository.findByBusinessIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(menu1, menu2));

        List<Menu> result = menuService.findByBusinessId(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Breakfast");
        assertThat(result.get(1).getName()).isEqualTo("Drinks");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(menuRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Menu> result = menuService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void updateMenu_renamesOwnedMenu() {
        Business business = new Business();
        business.setId(10L);
        Menu menu = new Menu();
        menu.setId(1L);
        menu.setBusinessId(10L);
        menu.setName("Breakfast");

        when(businessService.findByOwnerUserId(1L)).thenReturn(Optional.of(business));
        when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));
        when(menuRepository.save(org.mockito.ArgumentMatchers.any(Menu.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Menu result = menuService.updateMenu(1L, 1L, new UpdateMenuRequest("Brunch"));

        assertThat(result.getName()).isEqualTo("Brunch");
    }

    @Test
    void updateMenu_rejectsMenuFromAnotherBusiness() {
        Business business = new Business();
        business.setId(10L);
        Menu foreignMenu = new Menu();
        foreignMenu.setId(1L);
        foreignMenu.setBusinessId(999L);

        when(businessService.findByOwnerUserId(1L)).thenReturn(Optional.of(business));
        when(menuRepository.findById(1L)).thenReturn(Optional.of(foreignMenu));

        assertThatThrownBy(() -> menuService.updateMenu(1L, 1L, new UpdateMenuRequest("Brunch")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteMenu_deletesOwnedMenu() {
        Business business = new Business();
        business.setId(10L);
        Menu menu = new Menu();
        menu.setId(1L);
        menu.setBusinessId(10L);

        when(businessService.findByOwnerUserId(1L)).thenReturn(Optional.of(business));
        when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));

        menuService.deleteMenu(1L, 1L);

        verify(menuRepository).delete(menu);
    }

    @Test
    void deleteMenu_rejectsMenuFromAnotherBusiness() {
        Business business = new Business();
        business.setId(10L);
        Menu foreignMenu = new Menu();
        foreignMenu.setId(1L);
        foreignMenu.setBusinessId(999L);

        when(businessService.findByOwnerUserId(1L)).thenReturn(Optional.of(business));
        when(menuRepository.findById(1L)).thenReturn(Optional.of(foreignMenu));

        assertThatThrownBy(() -> menuService.deleteMenu(1L, 1L))
                .isInstanceOf(InvalidRequestException.class);
        verify(menuRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
