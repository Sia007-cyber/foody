package com.foody.auth.config;

import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Provides the HMAC-SHA key derived from the configured base64 secret.
 * The secret is 256-bit (jjwt enforces the minimum for HS256).
 */
@Configuration
public class JwtConfig {

    static final String DEVELOPMENT_SECRET =
            "Zm9vZHktcGhhc2UwLXNlY3JldC1rZXktZm9yLWhtYWMtYW5kLWFlc2VjLWtleWluZw==";
    static final int MINIMUM_HMAC_KEY_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(JwtProperties props, Environment environment) {
        String configuredSecret = props.getSecret();
        boolean production = environment.acceptsProfiles(Profiles.of("prod", "sharedhost"));

        if (configuredSecret == null || configuredSecret.isBlank()) {
            String message = production
                    ? "FOODY_JWT_SECRET is required in production and must not be blank"
                    : "foody.jwt.secret must not be blank";
            throw new IllegalStateException(message);
        }
        if (production && DEVELOPMENT_SECRET.equals(configuredSecret)) {
            throw new IllegalStateException(
                    "Production JWT secret must not use the committed development default");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(configuredSecret);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalStateException("FOODY_JWT_SECRET must be valid Base64");
        }
        if (keyBytes.length < MINIMUM_HMAC_KEY_BYTES) {
            throw new IllegalStateException(
                    "FOODY_JWT_SECRET must decode to at least 256 bits (32 bytes) for HMAC JWT signing");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
