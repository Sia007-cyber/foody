package com.foody.menus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl(menuRepository);
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
}
