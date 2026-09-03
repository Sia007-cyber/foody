package com.foody.menus.controller;

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
import com.foody.menus.dto.CreateMenuRequest;
import com.foody.menus.dto.UpdateMenuRequest;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
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
class MenuOwnerControllerTest {

    static final Long OWNER_ID = 1L;

    @Mock MenuService menuService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MenuOwnerController controller = new MenuOwnerController(menuService);

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

    private Menu sampleMenu() {
        Menu menu = new Menu();
        menu.setId(1L);
        menu.setBusinessId(10L);
        menu.setName("Breakfast");
        menu.setDisplayOrder(0);
        return menu;
    }

    @Test
    void createMenu_returnsCreatedMenu() throws Exception {
        when(menuService.createMenu(eq(OWNER_ID), any())).thenReturn(sampleMenu());

        CreateMenuRequest request = new CreateMenuRequest("Breakfast", 0);

        mockMvc.perform(post("/api/business/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Breakfast"))
                .andExpect(jsonPath("$.businessId").value(10));
    }

    @Test
    void createMenu_rejectsBlankName() throws Exception {
        String body = "{\"name\":\"\",\"displayOrder\":0}";

        mockMvc.perform(post("/api/business/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMenu_returns404WhenCallerHasNoBusiness() throws Exception {
        when(menuService.createMenu(eq(OWNER_ID), any()))
                .thenThrow(new ResourceNotFoundException("Business not found for owner"));

        CreateMenuRequest request = new CreateMenuRequest("Breakfast", 0);

        mockMvc.perform(post("/api/business/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMenu_returnsRenamedMenu() throws Exception {
        Menu renamed = sampleMenu();
        renamed.setName("Brunch");

        when(menuService.updateMenu(eq(OWNER_ID), eq(1L), any())).thenReturn(renamed);

        mockMvc.perform(patch("/api/business/menus/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMenuRequest("Brunch"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Brunch"));
    }

    @Test
    void updateMenu_returns404WhenNotOwnedByCaller() throws Exception {
        when(menuService.updateMenu(eq(OWNER_ID), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Menu not found for owner"));

        mockMvc.perform(patch("/api/business/menus/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMenuRequest("Brunch"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMenu_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/business/menus/1"))
                .andExpect(status().isNoContent());
    }
}
