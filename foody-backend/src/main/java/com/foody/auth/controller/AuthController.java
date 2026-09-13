package com.foody.auth.controller;

import com.foody.auth.dto.LoginRequest;
import com.foody.auth.dto.RefreshRequest;
import com.foody.auth.dto.RegisterRequest;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.auth.service.AdminAccountSupportService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AdminAccountSupportService accountSupportService;

    public AuthController(AuthService authService, AdminAccountSupportService accountSupportService) {
        this.authService = authService;
        this.accountSupportService = accountSupportService;
    }

    @PostMapping("/register")
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/impersonation/exit")
    @PreAuthorize("isAuthenticated()")
    public void exitImpersonation(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        accountSupportService.exitImpersonation(principal);
    }
}
