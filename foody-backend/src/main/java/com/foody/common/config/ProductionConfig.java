package com.foody.common.config;

import java.net.URI;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("prod")
public class ProductionConfig {
    public ProductionConfig(Environment env) {
        if (Arrays.stream(env.getActiveProfiles()).anyMatch(p -> p.equals("local") || p.equals("tc"))) {
            throw new IllegalStateException("prod cannot be combined with local or tc");
        }
        String origins = env.getRequiredProperty("foody.cors.allowed-origins");
        for (String origin : origins.split(",", -1)) {
            URI uri = URI.create(origin.trim());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty()) || uri.getQuery() != null
                    || uri.getFragment() != null || "localhost".equals(uri.getHost())) {
                throw new IllegalStateException("FOODY_CORS_ALLOWED_ORIGINS must contain explicit HTTPS origins without trailing slashes");
            }
        }
    }
}
