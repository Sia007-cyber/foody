package com.foody.businesses.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.businesses.dto.UpdateBusinessProfileRequest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.util.Optional;
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
class BusinessOwnerControllerTest {

    static final Long OWNER_ID = 1L;

    @Mock BusinessService businessService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        BusinessOwnerController controller = new BusinessOwnerController(businessService);

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

    private Business sampleBusiness() {
        Business business = new Business();
        business.setId(10L);
        business.setOwnerUserId(OWNER_ID);
        business.setName("Test Cafe");
        business.setBusinessType("CAFE");
        business.setStatus(BusinessStatus.APPROVED);
        return business;
    }

    @Test
    void getProfile_returnsOwnersBusiness() throws Exception {
        when(businessService.findByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(sampleBusiness()));

        mockMvc.perform(get("/api/business/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Cafe"));
    }

    @Test
    void getProfile_returns404WhenNoBusiness() throws Exception {
        when(businessService.findByOwnerUserId(OWNER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/business/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_returnsUpdatedBusiness() throws Exception {
        Business updated = sampleBusiness();
        updated.setName("Renamed Cafe");

        when(businessService.updateProfile(eq(OWNER_ID), any())).thenReturn(updated);

        UpdateBusinessProfileRequest request = new UpdateBusinessProfileRequest(
                "Renamed Cafe", null, null, null, null, null, null);

        mockMvc.perform(patch("/api/business/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Cafe"));
    }
}
