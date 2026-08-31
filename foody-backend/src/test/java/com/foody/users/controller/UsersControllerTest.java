package com.foody.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    static final Long USER_ID = 1L;

    @Mock UserService userService;
    @Mock PasswordEncoder passwordEncoder;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    User principalUser;

    @BeforeEach
    void setUp() {
        UsersController controller = new UsersController(userService, passwordEncoder);

        principalUser = new User();
        principalUser.setId(USER_ID);
        principalUser.setEmail("customer@foody.test");
        principalUser.setFullName("Test Customer");
        principalUser.setPhone("0912xxxxxxx");
        principalUser.setRole(UserRole.CUSTOMER);
        principalUser.setStatus(UserStatus.ACTIVE);
        FoodyUserPrincipal principal = new FoodyUserPrincipal(principalUser);

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

    @Test
    void me_returnsCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value("customer@foody.test"))
                .andExpect(jsonPath("$.fullName").value("Test Customer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void updateMe_appliesOnlyNonNullFieldsAndReturnsUpdatedProfile() throws Exception {
        when(userService.findById(USER_ID)).thenReturn(Optional.of(principalUser));
        when(userService.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String body = "{\"fullName\":\"Renamed Customer\"}";

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed Customer"))
                .andExpect(jsonPath("$.phone").value("0912xxxxxxx"));

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateMe_encodesPasswordWhenProvided() throws Exception {
        when(userService.findById(USER_ID)).thenReturn(Optional.of(principalUser));
        when(passwordEncoder.encode("newSecurePassword")).thenReturn("hashed-value");
        when(userService.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String body = "{\"password\":\"newSecurePassword\"}";

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(passwordEncoder).encode("newSecurePassword");
    }

    @Test
    void updateMe_rejectsTooShortPassword() throws Exception {
        String body = "{\"password\":\"short\"}";

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMe_returns404WhenUserMissing() throws Exception {
        when(userService.findById(USER_ID)).thenReturn(Optional.empty());

        String body = "{\"fullName\":\"Whoever\"}";

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
