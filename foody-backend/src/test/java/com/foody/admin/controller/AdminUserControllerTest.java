package com.foody.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.admin.service.AdminService;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
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
class AdminUserControllerTest {

    static final Long USER_ID = 20L;

    @Mock AdminService adminService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminUserController controller = new AdminUserController(adminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private User user(UserStatus status) {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("customer@foody.test");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(status);
        return user;
    }

    @Test
    void getUsers_returnsListFilteredByRoleAndStatus() throws Exception {
        when(adminService.getUsers(UserRole.CUSTOMER, UserStatus.ACTIVE))
                .thenReturn(List.of(user(UserStatus.ACTIVE)));

        mockMvc.perform(get("/api/admin/users").param("role", "CUSTOMER").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("customer@foody.test"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void suspend_returnsSuspendedUser() throws Exception {
        when(adminService.suspendUser(USER_ID)).thenReturn(user(UserStatus.SUSPENDED));

        mockMvc.perform(patch("/api/admin/users/{id}/suspend", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void activate_returnsActiveUser() throws Exception {
        when(adminService.activateUser(USER_ID)).thenReturn(user(UserStatus.ACTIVE));

        mockMvc.perform(patch("/api/admin/users/{id}/activate", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
