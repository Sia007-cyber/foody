package com.foody.common.config;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@Profile({"prod", "sharedhost", "vps"})
public class ProductionConfig {
    public ProductionConfig(Environment env) {
        if (Arrays.stream(env.getActiveProfiles()).anyMatch(p -> p.equals("local") || p.equals("tc"))) {
            throw new IllegalStateException("production profiles cannot be combined with local or tc");
        }
        long productionProfileCount = Arrays.stream(env.getActiveProfiles())
                .filter(p -> p.equals("prod") || p.equals("sharedhost") || p.equals("vps"))
                .count();
        if (productionProfileCount != 1) {
            throw new IllegalStateException("exactly one production profile must be active");
        }
        if (env.getProperty("foody.demo-accounts.enabled", Boolean.class, false)) {
            throw new IllegalStateException("demo accounts must never be enabled in production");
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
        if (env.acceptsProfiles(Profiles.of("sharedhost", "vps"))) {
            String uploadDir = env.getRequiredProperty("foody.storage.upload-dir");
            if (!Path.of(uploadDir).isAbsolute()) {
                throw new IllegalStateException("FOODY_STORAGE_LOCAL_PATH must be an absolute persistent path");
            }
        }
        if (env.acceptsProfiles(Profiles.of("vps"))) {
            if (!"127.0.0.1".equals(env.getRequiredProperty("server.address"))) {
                throw new IllegalStateException("the VPS backend must bind only to 127.0.0.1");
            }
            if (!"8080".equals(env.getRequiredProperty("server.port"))) {
                throw new IllegalStateException("the VPS backend must listen on port 8080");
            }
        }
    }
}
