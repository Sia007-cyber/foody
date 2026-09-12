package com.foody.auth.service;

import com.foody.auth.config.JwtProperties;
import com.foody.auth.dto.LoginRequest;
import com.foody.auth.dto.RegisterRequest;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.security.JwtService;
import com.foody.auth.entity.RefreshTokenSession;
import com.foody.auth.repository.RefreshTokenSessionRepository;
import com.foody.common.exception.DuplicateResourceException;
import com.foody.common.exception.InvalidCredentialsException;
import com.foody.common.exception.InvalidRequestException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenSessionRepository refreshTokenRepository;

    AuthServiceImpl(UserService userService, JwtService jwtService,
                    PasswordEncoder passwordEncoder, JwtProperties jwtProperties,
                    RefreshTokenSessionRepository refreshTokenRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new InvalidRequestException("Cannot self-register as ADMIN");
        }
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());
        if (email != null && userService.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (userService.existsByPhone(phone)) {
            throw new DuplicateResourceException("Phone is already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user = userService.create(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = identifier.matches("09\\d{9}")
                ? userService.findByPhone(identifier).orElseThrow(this::invalidCredentials)
                : userService.findByEmail(identifier.toLowerCase(java.util.Locale.ROOT)).orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw invalidCredentials();
        }
        return issueTokens(user);
    }

    private InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException("Invalid login identifier or password");
    }

    private static String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizePhone(String value) {
        String phone = value == null ? "" : value.trim();
        if (!phone.matches("09\\d{9}")) throw new InvalidRequestException("Phone must match 09xxxxxxxxx");
        return phone;
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken);
        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidCredentialsException("Provided token is not a refresh token");
        }
        String tokenHash = hash(refreshToken);
        RefreshTokenSession session = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is revoked or unknown"));
        Instant now = Instant.now();
        if (session.getRevokedAt() != null) {
            throw new InvalidCredentialsException("Refresh token has already been used or revoked");
        }
        if (!session.getExpiresAt().isAfter(now)) {
            throw new InvalidCredentialsException("Invalid or expired token");
        }
        Long userId = jwtService.getUserId(claims);
        if (userId == null) {
            throw new InvalidCredentialsException("Invalid account");
        }
        User user = session.getUser();
        if (!userId.equals(user.getId())) {
            throw new InvalidCredentialsException("Invalid account");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Account is suspended or disabled");
        }
        TokenResponse replacement = issueTokens(user);
        session.setRevokedAt(now);
        session.setReplacedByHash(hash(replacement.refreshToken()));
        refreshTokenRepository.save(session);
        return replacement;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken);
        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidCredentialsException("Provided token is not a refresh token");
        }
        RefreshTokenSession session = refreshTokenRepository.findByTokenHashForUpdate(hash(refreshToken))
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is revoked or unknown"));
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(Instant.now());
            refreshTokenRepository.save(session);
        }
    }

    @Override
    @Transactional
    public void revokeAllRefreshSessions(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        Claims refreshClaims = jwtService.parse(refresh);
        RefreshTokenSession session = new RefreshTokenSession();
        session.setUser(user);
        session.setTokenHash(hash(refresh));
        session.setExpiresAt(refreshClaims.getExpiration().toInstant());
        refreshTokenRepository.save(session);
        long expiresIn = Duration.ofMinutes(jwtProperties.getAccessTokenTtlMinutes()).getSeconds();
        return new TokenResponse(access, refresh, "Bearer", expiresIn);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
