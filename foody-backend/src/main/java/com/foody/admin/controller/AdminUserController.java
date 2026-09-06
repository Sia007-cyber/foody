package com.foody.admin.controller;

import com.foody.admin.service.AdminService;
import com.foody.users.dto.UserResponse;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin panel — user directory and moderation. Suspend an account to immediately
 * block its login (see FoodyUserPrincipal.isEnabled/isAccountNonLocked), or
 * reactivate a previously suspended one. Admin accounts cannot be targeted
 * (see UserServiceImpl.updateStatus).
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<UserResponse> getUsers(@RequestParam(required = false) UserRole role,
                                        @RequestParam(required = false) UserStatus status) {
        return adminService.getUsers(role, status).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/suspend")
    public UserResponse suspend(@PathVariable Long id) {
        User user = adminService.suspendUser(id);
        return UserResponse.from(user);
    }

    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        User user = adminService.activateUser(id);
        return UserResponse.from(user);
    }
}
