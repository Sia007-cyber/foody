package com.foody.products.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
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
    @Mock BusinessService businessService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService, menuService, businessService);
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
        Menu menu = menu(5L, 10L);
        when(menuService.findById(5L)).thenReturn(Optional.of(menu));
        when(businessService.findByIdAndStatus(10L, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(business(10L)));

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
        Menu menu = menu(5L, 10L);

        Product product = new Product();
        product.setId(1L);
        product.setMenuId(5L);
        product.setName("Latte");
        product.setPrice(new BigDecimal("4.50"));

        when(menuService.findById(5L)).thenReturn(Optional.of(menu));
        when(businessService.findByIdAndStatus(10L, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(business(10L)));
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

    @Test
    void getById_returns404WhenParentBusinessIsNotPublic() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setMenuId(5L);
        when(productService.findById(1L)).thenReturn(Optional.of(product));
        when(menuService.findById(5L)).thenReturn(Optional.of(menu(5L, 10L)));
        when(businessService.findByIdAndStatus(10L, BusinessStatus.APPROVED))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductsForMenu_returns404WhenParentBusinessIsNotPublic() throws Exception {
        when(menuService.findById(5L)).thenReturn(Optional.of(menu(5L, 10L)));
        when(businessService.findByIdAndStatus(10L, BusinessStatus.APPROVED))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/menus/5/products"))
                .andExpect(status().isNotFound());
    }

    private Menu menu(Long id, Long businessId) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setBusinessId(businessId);
        menu.setName("Drinks");
        return menu;
    }

    private Business business(Long id) {
        Business business = new Business();
        business.setId(id);
        business.setStatus(BusinessStatus.APPROVED);
        return business;
    }
}
