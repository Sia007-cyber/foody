package com.foody.menus.controller;

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
class MenuControllerTest {

    @Mock MenuService menuService;
    @Mock BusinessService businessService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MenuController controller = new MenuController(menuService, businessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMenusForBusiness_returnsMenusForApprovedBusiness() throws Exception {
        Business business = new Business();
        business.setId(10L);
        business.setStatus(BusinessStatus.APPROVED);

        Menu menu = new Menu();
        menu.setId(1L);
        menu.setBusinessId(10L);
        menu.setName("Breakfast");
        menu.setDisplayOrder(0);

        when(businessService.findByIdAndStatus(10L, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(business));
        when(menuService.findByBusinessId(10L)).thenReturn(List.of(menu));

        mockMvc.perform(get("/api/businesses/10/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Breakfast"));
    }

    @Test
    void getMenusForBusiness_returns404WhenBusinessNotApproved() throws Exception {
        when(businessService.findByIdAndStatus(99L, BusinessStatus.APPROVED))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/businesses/99/menus"))
                .andExpect(status().isNotFound());
    }
}
