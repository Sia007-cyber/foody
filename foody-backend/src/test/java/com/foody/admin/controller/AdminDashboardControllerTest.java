package com.foody.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.admin.dto.DashboardSummaryResponse;
import com.foody.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock AdminService adminService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminDashboardController controller = new AdminDashboardController(adminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void summary_returnsAggregatedCounts() throws Exception {
        when(adminService.getDashboardSummary())
                .thenReturn(new DashboardSummaryResponse(42L, 7L, 123L, 58L));

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(42))
                .andExpect(jsonPath("$.activeBusinesses").value(7))
                .andExpect(jsonPath("$.totalOrders").value(123))
                .andExpect(jsonPath("$.totalReservations").value(58));
    }
}
