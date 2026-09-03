package com.foody.products.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.businesses.entity.Business;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
import com.foody.products.dto.CreateProductRequest;
import com.foody.products.dto.UpdateProductRequest;
import com.foody.products.entity.Product;
import com.foody.products.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock MenuService menuService;
    @Mock BusinessService businessService;

    ProductServiceImpl productService;

    static final Long OWNER_ID = 1L;
    static final Long BUSINESS_ID = 10L;
    static final Long MENU_ID = 20L;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, menuService, businessService);
    }

    private Business ownedBusiness() {
        Business business = new Business();
        business.setId(BUSINESS_ID);
        return business;
    }

    private Menu ownedMenu() {
        Menu menu = new Menu();
        menu.setId(MENU_ID);
        menu.setBusinessId(BUSINESS_ID);
        return menu;
    }

    @Test
    void createProduct_savesUnderOwnedMenu() {
        when(businessService.findByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(ownedBusiness()));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(ownedMenu()));
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateProductRequest request = new CreateProductRequest(
                MENU_ID, "Latte", "Hot milk coffee", new BigDecimal("4.50"), null, null);

        Product result = productService.createProduct(OWNER_ID, request);

        assertThat(result.getMenuId()).isEqualTo(MENU_ID);
        assertThat(result.getIsAvailable()).isTrue();
    }

    @Test
    void createProduct_rejectsMenuFromAnotherBusiness() {
        Menu foreignMenu = new Menu();
        foreignMenu.setId(MENU_ID);
        foreignMenu.setBusinessId(999L);

        when(businessService.findByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(ownedBusiness()));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(foreignMenu));

        CreateProductRequest request = new CreateProductRequest(
                MENU_ID, "Latte", null, new BigDecimal("4.50"), null, null);

        assertThatThrownBy(() -> productService.createProduct(OWNER_ID, request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateProduct_appliesOnlyNonNullFields() {
        Product existing = new Product();
        existing.setId(30L);
        existing.setMenuId(MENU_ID);
        existing.setName("Latte");
        existing.setPrice(new BigDecimal("4.50"));
        existing.setIsAvailable(true);

        when(productRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(businessService.findByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(ownedBusiness()));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(ownedMenu()));
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest(null, null, null, null, false, null);
        Product result = productService.updateProduct(OWNER_ID, 30L, request);

        assertThat(result.getName()).isEqualTo("Latte");
        assertThat(result.getIsAvailable()).isFalse();
    }

    @Test
    void findById_returnsProductWhenFound() {
        Product product = new Product();
        product.setId(1L);
        product.setMenuId(5L);
        product.setName("Latte");
        product.setPrice(new BigDecimal("4.50"));
        product.setIsAvailable(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Optional<Product> result = productService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Latte");
    }

    @Test
    void findByMenuId_returnsProductsOrderedByDisplayOrder() {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setMenuId(5L);
        p1.setName("Latte");
        p1.setDisplayOrder(0);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setMenuId(5L);
        p2.setName("Cappuccino");
        p2.setDisplayOrder(1);

        when(productRepository.findByMenuIdOrderByDisplayOrderAsc(5L))
                .thenReturn(List.of(p1, p2));

        List<Product> result = productService.findByMenuId(5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Latte");
    }

    @Test
    void deleteProduct_deletesOwnedProduct() {
        Product product = new Product();
        product.setId(30L);
        product.setMenuId(MENU_ID);

        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(businessService.findByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(ownedBusiness()));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(ownedMenu()));

        productService.deleteProduct(OWNER_ID, 30L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_rejectsProductFromMenuOfAnotherBusiness() {
        Product product = new Product();
        product.setId(30L);
        product.setMenuId(MENU_ID);

        Menu foreignMenu = new Menu();
        foreignMenu.setId(MENU_ID);
        foreignMenu.setBusinessId(999L);

        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(businessService.findByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(ownedBusiness()));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(foreignMenu));

        assertThatThrownBy(() -> productService.deleteProduct(OWNER_ID, 30L))
                .isInstanceOf(InvalidRequestException.class);
        verify(productRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
