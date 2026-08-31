package com.foody.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foody.auth.config.JwtProperties;
import com.foody.common.exception.InvalidCredentialsException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import io.jsonwebtoken.Claims;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class JwtServiceTest {

    JwtService jwtService;
    User user;

    @BeforeEach
    void setUp() {
        byte[] key = Base64.getDecoder().decode(
                "Zm9vZHktcGhhc2UwLXNlY3JldC1rZXktZm9yLWhtYWMtYW5kLWFlc2VjLWtleWluZw==");
        SecretKey secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(key);
        JwtProperties props = new JwtProperties();
        props.setSecret("Zm9vZHktcGhhc2UwLXNlY3JldC1rZXktZm9yLWhtYWMtYW5kLWFlc2VjLWtleWluZw==");
        props.setAccessTokenTtlMinutes(15);
        props.setRefreshTokenTtlDays(7);
        jwtService = new JwtService(secretKey, props);

        user = new User();
        user.setId(42L);
        user.setEmail("a@b.com");
        user.setFullName("Alice");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("x"));
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void accessAndRefreshTokens_roundTripAndDifferByType() {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        Claims ac = jwtService.parse(access);
        Claims rc = jwtService.parse(refresh);

        assertThat(jwtService.getUserId(ac)).isEqualTo(42L);
        assertThat(jwtService.getRole(ac)).isEqualTo("CUSTOMER");
        assertThat(jwtService.isRefreshToken(ac)).isFalse();
        assertThat(jwtService.isRefreshToken(rc)).isTrue();
        assertThat(access).isNotEqualTo(refresh);
    }

    @Test
    void tamperedToken_rejected() {
        String access = jwtService.generateAccessToken(user) + "tamper";
        assertThatThrownBy(() -> jwtService.parse(access))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
