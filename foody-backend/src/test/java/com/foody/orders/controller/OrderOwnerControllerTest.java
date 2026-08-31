package com.foody.orders.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.orders.dto.OrderItemResponse;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.dto.UpdateOrderStatusRequest;
import com.foody.orders.entity.FulfillmentType;
import com.foody.orders.entity.OrderStatus;
import com.foody.orders.service.OrderService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class OrderOwnerControllerTest {

    static final Long OWNER_ID = 1L;

    @Mock OrderService orderService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        OrderOwnerController controller = new OrderOwnerController(orderService);

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

    private OrderResponse sampleOrder(OrderStatus status) {
        return new OrderResponse(
                100L, 10L, 5L, FulfillmentType.PICKUP, status, null,
                new BigDecimal("9.00"),
                List.of(new OrderItemResponse(30L, "Latte", new BigDecimal("4.50"), 2, new BigDecimal("9.00"))),
                Instant.now(), Instant.now());
    }

    @Test
    void getBusinessOrders_returnsOwnersOrders() throws Exception {
        when(orderService.getBusinessOrders(OWNER_ID, null)).thenReturn(List.of(sampleOrder(OrderStatus.PENDING)));

        mockMvc.perform(get("/api/business/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getBusinessOrders_filtersByStatus() throws Exception {
        when(orderService.getBusinessOrders(OWNER_ID, OrderStatus.READY))
                .thenReturn(List.of(sampleOrder(OrderStatus.READY)));

        mockMvc.perform(get("/api/business/orders").param("status", "READY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("READY"));
    }

    @Test
    void updateStatus_returnsUpdatedOrder() throws Exception {
        when(orderService.updateOrderStatus(eq(OWNER_ID), eq(100L), eq(OrderStatus.ACCEPTED)))
                .thenReturn(sampleOrder(OrderStatus.ACCEPTED));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);

        mockMvc.perform(patch("/api/business/orders/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void updateStatus_rejectsMissingStatus() throws Exception {
        String body = "{}";

        mockMvc.perform(patch("/api/business/orders/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_returnsConflictOnInvalidTransition() throws Exception {
        when(orderService.updateOrderStatus(eq(OWNER_ID), eq(100L), eq(OrderStatus.COMPLETED)))
                .thenThrow(new InvalidStateTransitionException("Order 100 cannot move to COMPLETED from PENDING"));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.COMPLETED);

        mockMvc.perform(patch("/api/business/orders/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_returns404WhenOrderNotOwnedByCaller() throws Exception {
        when(orderService.updateOrderStatus(eq(OWNER_ID), eq(999L), eq(OrderStatus.ACCEPTED)))
                .thenThrow(new ResourceNotFoundException("Order not found for owner"));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);

        mockMvc.perform(patch("/api/business/orders/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
