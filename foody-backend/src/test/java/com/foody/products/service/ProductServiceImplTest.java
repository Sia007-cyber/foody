package com.foody.products.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository);
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
}
