package com.foody.auth.security;

import com.foody.auth.config.JwtProperties;
import com.foody.users.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies stateless JWTs. Two token kinds share the same signing key
 * but differ by the {@code typ} claim and lifetime:
 *  - access : short-lived (default 15 min), grants API access
 *  - refresh: long-lived (default 7 days), used only to mint a new access token
 *
 * Stateless by design: no token store. /logout is handled client-side (token drop).
 * Revocation of a single refresh token requires state (Phase 2+ work).
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(SecretKey key, JwtProperties props) {
        this.key = key;
        this.props = props;
    }

    public String generateAccessToken(User user) {
        return build(user, "access", props.getAccessTokenTtlMinutes() * 60L);
    }

    public String generateRefreshToken(User user) {
        return build(user, "refresh", props.getRefreshTokenTtlDays() * 24L * 60L * 60L);
    }

    private String build(User user, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    /** Parse + validate any token, returning its claims or throwing on failure. */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new com.foody.common.exception.InvalidCredentialsException("Invalid or expired token");
        }
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get(CLAIM_TYPE, String.class));
    }

    public Long getUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }
}
