package com.foody.products.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
import com.foody.products.entity.Product;
import com.foody.products.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock ProductService productService;
    @Mock MenuService menuService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService, menuService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getById_returnsProduct() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setMenuId(5L);
        product.setName("Latte");
        product.setPrice(new BigDecimal("4.50"));
        product.setIsAvailable(true);

        when(productService.findById(1L)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Latte"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(productService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductsForMenu_returnsProductsWhenMenuExists() throws Exception {
        Menu menu = new Menu();
        menu.setId(5L);
        menu.setBusinessId(10L);
        menu.setName("Drinks");

        Product product = new Product();
        product.setId(1L);
        product.setMenuId(5L);
        product.setName("Latte");
        product.setPrice(new BigDecimal("4.50"));

        when(menuService.findById(5L)).thenReturn(Optional.of(menu));
        when(productService.findByMenuId(5L)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/menus/5/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Latte"));
    }

    @Test
    void getProductsForMenu_returns404WhenMenuMissing() throws Exception {
        when(menuService.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/menus/404/products"))
                .andExpect(status().isNotFound());
    }
}
