package com.foody.auth.dto;

/** Returned by register/login/refresh. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds) {
}
