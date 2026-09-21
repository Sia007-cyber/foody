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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.auth.service.AdminAccountSupportService;
import com.foody.admin.dto.AdminPasswordResetRequest;
import jakarta.validation.Valid;
import com.foody.admin.dto.AdminUserDetailResponse;
import com.foody.admin.service.PrimaryAdminRoleService;

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
    private final AdminAccountSupportService accountSupportService;
    private final PrimaryAdminRoleService primaryAdminRoleService;

    public AdminUserController(AdminService adminService, AdminAccountSupportService accountSupportService,
                               PrimaryAdminRoleService primaryAdminRoleService) {
        this.adminService = adminService;
        this.accountSupportService = accountSupportService;
        this.primaryAdminRoleService = primaryAdminRoleService;
    }

    @GetMapping
    public List<UserResponse> getUsers(@RequestParam(required = false) UserRole role,
                                        @RequestParam(required = false) UserStatus status) {
        return adminService.getUsers(role, status).stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AdminUserDetailResponse getUser(@PathVariable Long id) {
        return adminService.getUserDetail(id);
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

    @PostMapping("/{id}/impersonate")
    public TokenResponse impersonate(@AuthenticationPrincipal FoodyUserPrincipal principal, @PathVariable Long id) {
        return accountSupportService.startImpersonation(principal, id);
    }

    @PostMapping("/{id}/password-reset")
    public void resetPassword(@AuthenticationPrincipal FoodyUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody AdminPasswordResetRequest request) {
        accountSupportService.resetPassword(principal, id, request.newPassword());
    }

    @PatchMapping("/{id}/grant-admin")
    public UserResponse grantAdmin(@AuthenticationPrincipal FoodyUserPrincipal principal, @PathVariable Long id) {
        return UserResponse.from(primaryAdminRoleService.grantAdmin(principal, id));
    }

    @PatchMapping("/{id}/revoke-admin")
    public UserResponse revokeAdmin(@AuthenticationPrincipal FoodyUserPrincipal principal, @PathVariable Long id) {
        return UserResponse.from(primaryAdminRoleService.revokeAdmin(principal, id));
    }
}
