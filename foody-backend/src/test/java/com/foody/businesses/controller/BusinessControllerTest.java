package com.foody.businesses.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.GlobalExceptionHandler;
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
class BusinessControllerTest {

    @Mock BusinessService businessService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BusinessController controller = new BusinessController(businessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Business approvedBusiness(Long id, String name, String type) {
        Business business = new Business();
        business.setId(id);
        business.setName(name);
        business.setBusinessType(type);
        business.setStatus(BusinessStatus.APPROVED);
        return business;
    }

    @Test
    void getById_returnsBusinessWhenApproved() throws Exception {
        when(businessService.findByIdAndStatus(1L, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness(1L, "Cafe Sunrise", "CAFE")));

        mockMvc.perform(get("/api/businesses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cafe Sunrise"));
    }

    @Test
    void getById_returns404WhenNotApproved() throws Exception {
        when(businessService.findByIdAndStatus(99L, BusinessStatus.APPROVED))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/businesses/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void discover_withNoFilters_returnsAllApprovedBusinesses() throws Exception {
        when(businessService.search(isNull(), isNull())).thenReturn(List.of(
                approvedBusiness(1L, "Cafe Sunrise", "CAFE"),
                approvedBusiness(2L, "Burger Town", "FAST_FOOD")));

        mockMvc.perform(get("/api/businesses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void discover_passesTypeAndSearchQueryParamsToService() throws Exception {
        when(businessService.search(eq("CAFE"), eq("sun"))).thenReturn(List.of(
                approvedBusiness(1L, "Cafe Sunrise", "CAFE")));

        mockMvc.perform(get("/api/businesses").param("type", "CAFE").param("search", "sun"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Cafe Sunrise"));
    }

    @Test
    void discover_returnsEmptyListWhenNoneMatch() throws Exception {
        when(businessService.search(eq("BAKERY"), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/businesses").param("type", "BAKERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
