package com.foody.auth.service;

import com.foody.auth.config.JwtProperties;
import com.foody.auth.dto.LoginRequest;
import com.foody.auth.dto.RegisterRequest;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.security.JwtService;
import com.foody.common.exception.DuplicateResourceException;
import com.foody.common.exception.InvalidCredentialsException;
import com.foody.common.exception.InvalidRequestException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    AuthServiceImpl(UserService userService, JwtService jwtService,
                    PasswordEncoder passwordEncoder, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new InvalidRequestException("Cannot self-register as ADMIN");
        }
        if (userService.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        User user = new User();
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user = userService.create(user);
        return issueTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new InvalidCredentialsException("Account is suspended");
        }
        return issueTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken);
        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidCredentialsException("Provided token is not a refresh token");
        }
        Long userId = jwtService.getUserId(claims);
        if (userId == null) {
            throw new InvalidCredentialsException("Invalid account");
        }
        User user = userService.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid account"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Account is suspended or disabled");
        }
        return issueTokens(user);
    }

    @Override
    public void logout() {
        // Stateless JWTs: no server-side revocation in Phase 0. The client discards
        // both tokens. A future phase can add a token-version / denylist for revocation.
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        long expiresIn = Duration.ofMinutes(jwtProperties.getAccessTokenTtlMinutes()).getSeconds();
        return new TokenResponse(access, refresh, "Bearer", expiresIn);
    }
}
