package com.foody.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.admin.service.AdminService;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.common.exception.GlobalExceptionHandler;
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
class AdminBusinessControllerTest {

    static final Long BUSINESS_ID = 10L;

    @Mock AdminService adminService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminBusinessController controller = new AdminBusinessController(adminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Business business(BusinessStatus status) {
        Business business = new Business();
        business.setId(BUSINESS_ID);
        business.setName("Test Cafe");
        business.setBusinessType("CAFE");
        business.setStatus(status);
        return business;
    }

    @Test
    void getBusinesses_returnsListFilteredByStatus() throws Exception {
        when(adminService.getBusinesses(BusinessStatus.PENDING))
                .thenReturn(List.of(business(BusinessStatus.PENDING)));

        mockMvc.perform(get("/api/admin/businesses").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Cafe"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void approve_returnsApprovedBusiness() throws Exception {
        when(adminService.approveBusiness(BUSINESS_ID)).thenReturn(business(BusinessStatus.APPROVED));

        mockMvc.perform(patch("/api/admin/businesses/{id}/approve", BUSINESS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reject_returnsRejectedBusiness() throws Exception {
        when(adminService.rejectBusiness(BUSINESS_ID)).thenReturn(business(BusinessStatus.REJECTED));

        mockMvc.perform(patch("/api/admin/businesses/{id}/reject", BUSINESS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void suspend_returnsSuspendedBusiness() throws Exception {
        when(adminService.suspendBusiness(BUSINESS_ID)).thenReturn(business(BusinessStatus.SUSPENDED));

        mockMvc.perform(patch("/api/admin/businesses/{id}/suspend", BUSINESS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void homepagePlacement_returnsUpdatedBusiness() throws Exception {
        Business featured = business(BusinessStatus.APPROVED);
        featured.setFeatured(true);
        Business popular = business(BusinessStatus.APPROVED);
        popular.setPopular(true);
        when(adminService.setBusinessFeatured(BUSINESS_ID, true)).thenReturn(featured);
        when(adminService.setBusinessPopular(BUSINESS_ID, true)).thenReturn(popular);

        mockMvc.perform(patch("/api/admin/businesses/{id}/featured", BUSINESS_ID)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.featured").value(true));
        mockMvc.perform(patch("/api/admin/businesses/{id}/popular", BUSINESS_ID)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.popular").value(true));
    }
}
