package com.foody.orders.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.orders.dto.CreateOrderRequest;
import com.foody.orders.dto.OrderItemRequest;
import com.foody.orders.dto.OrderItemResponse;
import com.foody.orders.dto.OrderResponse;
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

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    static final Long CUSTOMER_ID = 1L;

    @Mock OrderService orderService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(orderService);

        User user = new User();
        user.setId(CUSTOMER_ID);
        user.setEmail("customer@foody.test");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        FoodyUserPrincipal principal = new FoodyUserPrincipal(user);

        // Standalone MockMvc doesn't run the real security filter chain, so we stub
        // resolution of @AuthenticationPrincipal the same way Spring Security would.
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
                100L, 10L, CUSTOMER_ID, FulfillmentType.PICKUP, status, null,
                new BigDecimal("9.00"),
                List.of(new OrderItemResponse(30L, "Latte", new BigDecimal("4.50"), 2, new BigDecimal("9.00"))),
                Instant.now(), Instant.now());
    }

    @Test
    void createOrder_returnsCreatedOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                10L, FulfillmentType.PICKUP, List.of(new OrderItemRequest(30L, 2)), null);

        when(orderService.createOrder(eq(CUSTOMER_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleOrder(OrderStatus.PENDING));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(9.00));
    }

    @Test
    void createOrder_rejectsEmptyItems() throws Exception {
        String body = "{\"businessId\":10,\"fulfillmentType\":\"PICKUP\",\"items\":[]}";

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myOrders_returnsCustomersOrders() throws Exception {
        when(orderService.getMyOrders(CUSTOMER_ID)).thenReturn(List.of(sampleOrder(OrderStatus.PENDING)));

        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void cancel_returnsUpdatedOrder() throws Exception {
        when(orderService.cancelOrder(100L, CUSTOMER_ID)).thenReturn(sampleOrder(OrderStatus.CANCELLED));

        mockMvc.perform(patch("/api/orders/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_returnsConflictWhenNotPending() throws Exception {
        when(orderService.cancelOrder(100L, CUSTOMER_ID))
                .thenThrow(new InvalidStateTransitionException("Order 100 can no longer be cancelled"));

        mockMvc.perform(patch("/api/orders/100/cancel"))
                .andExpect(status().isConflict());
    }
}
