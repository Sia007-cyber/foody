package com.foody.products.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.products.dto.CreateProductRequest;
import com.foody.products.dto.UpdateProductRequest;
import com.foody.products.entity.Product;
import com.foody.products.service.ProductService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Note: standalone MockMvc doesn't process @PreAuthorize (no method-security
 * interceptor wired), so these tests exercise request/response mapping only.
 * The real authorization guard is the ownership check inside the service layer.
 */
@ExtendWith(MockitoExtension.class)
class ProductOwnerControllerTest {

    static final Long OWNER_ID = 1L;

    @Mock ProductService productService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ProductOwnerController controller = new ProductOwnerController(productService);

        User user = new User();
        user.setId(OWNER_ID);
        user.setEmail("owner@foody.test");
        user.setFullName("Test Owner");
        user.setRole(UserRole.BUSINESS_OWNER);
        user.setStatus(UserStatus.ACTIVE);
        FoodyUserPrincipal principal = new FoodyUserPrincipal(user);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(FoodyUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    private Product sampleProduct() {
        Product product = new Product();
        product.setId(30L);
        product.setMenuId(1L);
        product.setName("Latte");
        product.setDescription("Espresso with steamed milk");
        product.setPrice(new BigDecimal("4.50"));
        product.setIsAvailable(true);
        product.setDisplayOrder(0);
        return product;
    }

    @Test
    void createProduct_returnsCreatedProduct() throws Exception {
        when(productService.createProduct(eq(OWNER_ID), any())).thenReturn(sampleProduct());

        CreateProductRequest request = new CreateProductRequest(
                1L, "Latte", "Espresso with steamed milk", new BigDecimal("4.50"), null, 0);

        mockMvc.perform(post("/api/business/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Latte"))
                .andExpect(jsonPath("$.price").value(4.50));
    }

    @Test
    void createProduct_rejectsNegativePrice() throws Exception {
        String body = "{\"menuId\":1,\"name\":\"Latte\",\"price\":-1}";

        mockMvc.perform(post("/api/business/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_returns404WhenMenuNotOwnedByCaller() throws Exception {
        when(productService.createProduct(eq(OWNER_ID), any()))
                .thenThrow(new ResourceNotFoundException("Menu not found for owner"));

        CreateProductRequest request = new CreateProductRequest(
                1L, "Latte", null, new BigDecimal("4.50"), null, 0);

        mockMvc.perform(post("/api/business/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_returnsUpdatedProduct() throws Exception {
        Product updated = sampleProduct();
        updated.setName("Renamed Latte");
        updated.setIsAvailable(false);

        when(productService.updateProduct(eq(OWNER_ID), eq(30L), any())).thenReturn(updated);

        UpdateProductRequest request = new UpdateProductRequest(
                "Renamed Latte", null, null, null, false, null);

        mockMvc.perform(patch("/api/business/products/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Latte"))
                .andExpect(jsonPath("$.isAvailable").value(false));
    }

    @Test
    void updateProduct_returns404WhenProductNotOwnedByCaller() throws Exception {
        when(productService.updateProduct(eq(OWNER_ID), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Product not found for owner"));

        UpdateProductRequest request = new UpdateProductRequest(
                "Whatever", null, null, null, null, null);

        mockMvc.perform(patch("/api/business/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/business/products/30"))
                .andExpect(status().isNoContent());
    }
}
