package com.foody.auth.service;

import com.foody.auth.dto.TokenResponse;

/**
 * Public contract for authentication. Controllers and other modules depend on this
 * interface only. Implementations live in the auth module.
 */
public interface AuthService {

    TokenResponse register(com.foody.auth.dto.RegisterRequest request);

    TokenResponse login(com.foody.auth.dto.LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout();
}
