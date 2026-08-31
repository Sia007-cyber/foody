package com.foody.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.auth.config.JwtProperties;
import com.foody.auth.dto.LoginRequest;
import com.foody.auth.dto.RegisterRequest;
import com.foody.auth.dto.TokenResponse;
import com.foody.auth.security.JwtService;
import com.foody.common.exception.DuplicateResourceException;
import com.foody.common.exception.InvalidCredentialsException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserService userService;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProperties jwtProperties;

    AuthServiceImpl authService;
    final String ACCESS = "access.jwt.token";
    final String REFRESH = "refresh.jwt.token";

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userService, jwtService, passwordEncoder, jwtProperties);
        // lenient: not every test exercises token issuance.
        org.mockito.Mockito.lenient().when(jwtProperties.getAccessTokenTtlMinutes()).thenReturn(15L);
        org.mockito.Mockito.lenient().when(jwtService.generateAccessToken(any())).thenReturn(ACCESS);
        org.mockito.Mockito.lenient().when(jwtService.generateRefreshToken(any())).thenReturn(REFRESH);
    }

    @Test
    void register_createsCustomerAndReturnsTokens() {
        RegisterRequest req = new RegisterRequest("a@b.com", "password123", "Alice", "123");

        when(userService.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userService.create(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenResponse resp = authService.register(req);

        assertThat(resp.accessToken()).isEqualTo(ACCESS);
        assertThat(resp.refreshToken()).isEqualTo(REFRESH);
        assertThat(resp.tokenType()).isEqualTo("Bearer");
        verify(userService).create(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest("a@b.com", "password123", "Alice", null);
        when(userService.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userService, never()).create(any());
    }

    @Test
    void login_success_returnsTokens() {
        User user = makeUser(1L, UserStatus.ACTIVE);
        LoginRequest req = new LoginRequest("a@b.com", "password123");
        when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);

        TokenResponse resp = authService.login(req);

        assertThat(resp.accessToken()).isEqualTo(ACCESS);
    }

    @Test
    void login_wrongPassword_throws() {
        User user = makeUser(1L, UserStatus.ACTIVE);
        LoginRequest req = new LoginRequest("a@b.com", "wrong");
        when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_suspendedAccount_throws() {
        User user = makeUser(1L, UserStatus.SUSPENDED);
        LoginRequest req = new LoginRequest("a@b.com", "password123");
        when(userService.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_withRefreshToken_issuesNewTokens() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parse(REFRESH)).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(jwtService.getUserId(claims)).thenReturn(1L);
        User user = makeUser(1L, UserStatus.ACTIVE);
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        TokenResponse resp = authService.refresh(REFRESH);

        assertThat(resp.accessToken()).isEqualTo(ACCESS);
        assertThat(resp.refreshToken()).isEqualTo(REFRESH);
    }

    @Test
    void refresh_withAccessToken_throws() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parse(REFRESH)).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(false); // it's an access token

        assertThatThrownBy(() -> authService.refresh(REFRESH))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private User makeUser(Long id, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setEmail("a@b.com");
        u.setFullName("Alice");
        u.setPasswordHash("hashed");
        u.setRole(UserRole.CUSTOMER);
        u.setStatus(status);
        return u;
    }
}
