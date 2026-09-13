package com.foody.auth.security;

import com.foody.auth.config.JwtProperties;
import com.foody.users.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies stateless JWTs. Two token kinds share the same signing key
 * but differ by the {@code typ} claim and lifetime:
 *  - access : short-lived (default 15 min), grants API access
 *  - refresh: long-lived (default 7 days), used only to mint a new access token
 *
 * Access tokens remain stateless. Refresh tokens are tracked by hash server-side.
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_IMPERSONATION = "impersonation";
    private static final String CLAIM_INITIATING_ADMIN_ID = "initiatingAdminId";
    private static final String CLAIM_IMPERSONATION_SESSION_ID = "impersonationSessionId";
    private static final long IMPERSONATION_TTL_SECONDS = 30 * 60L;

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

    public String generateImpersonationAccessToken(User user, Long adminId, String sessionId) {
        return build(user, "access", IMPERSONATION_TTL_SECONDS, adminId, sessionId);
    }

    public String generateImpersonationRefreshToken(User user, Long adminId, String sessionId) {
        return build(user, "refresh", IMPERSONATION_TTL_SECONDS, adminId, sessionId);
    }

    private String build(User user, String type, long ttlSeconds) {
        return build(user, type, ttlSeconds, null, null);
    }

    private String build(User user, String type, long ttlSeconds, Long adminId, String sessionId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.getEmail() != null ? user.getEmail() : user.getPhone())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now));
        if (adminId != null && sessionId != null) {
            builder.claim(CLAIM_IMPERSONATION, true)
                    .claim(CLAIM_INITIATING_ADMIN_ID, adminId)
                    .claim(CLAIM_IMPERSONATION_SESSION_ID, sessionId);
        }
        return builder.expiration(Date.from(now.plusSeconds(ttlSeconds)))
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

    public boolean isImpersonation(Claims claims) {
        return Boolean.TRUE.equals(claims.get(CLAIM_IMPERSONATION, Boolean.class));
    }

    public Long getInitiatingAdminId(Claims claims) {
        return claims.get(CLAIM_INITIATING_ADMIN_ID, Long.class);
    }

    public String getImpersonationSessionId(Claims claims) {
        return claims.get(CLAIM_IMPERSONATION_SESSION_ID, String.class);
    }
}
