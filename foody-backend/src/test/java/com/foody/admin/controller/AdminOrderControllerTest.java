package com.foody.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.admin.dto.AdminOrderResponse;
import com.foody.admin.service.AdminService;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.orders.entity.FulfillmentType;
import com.foody.orders.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Note: standalone MockMvc doesn't process @PreAuthorize (no method-security
 * interceptor wired), so these tests exercise request/response mapping only.
 * The real ADMIN-only guard is verified by Spring Security at runtime.
 */
@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    static final Long BUSINESS_ID = 10L;

    @Mock AdminService adminService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminOrderController controller = new AdminOrderController(adminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AdminOrderResponse order() {
        return new AdminOrderResponse(
                100L, BUSINESS_ID, "Test Cafe", 1L, "Ali Rezaei", "ali@example.com", "0912",
                FulfillmentType.PICKUP, OrderStatus.PENDING, null, new BigDecimal("9.00"),
                List.of(), null, null);
    }

    @Test
    void getOrders_returnsListFilteredByStatusAndBusiness() throws Exception {
        when(adminService.getOrders(OrderStatus.PENDING, BUSINESS_ID)).thenReturn(List.of(order()));

        mockMvc.perform(get("/api/admin/orders")
                        .param("status", "PENDING")
                        .param("businessId", String.valueOf(BUSINESS_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessName").value("Test Cafe"))
                .andExpect(jsonPath("$[0].customerName").value("Ali Rezaei"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getOrders_worksWithoutFilters() throws Exception {
        when(adminService.getOrders(null, null)).thenReturn(List.of(order()));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }
}
