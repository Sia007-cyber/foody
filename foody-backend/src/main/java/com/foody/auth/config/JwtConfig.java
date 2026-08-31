package com.foody.auth.config;

import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the HMAC-SHA key derived from the configured base64 secret.
 * The secret is 256-bit (jjwt enforces the minimum for HS256).
 */
@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(JwtProperties props) {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(props.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
