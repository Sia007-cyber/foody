package com.foody.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Bound from {@code foody.jwt.*} in application.yml / env. */
@Component
@ConfigurationProperties(prefix = "foody.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenTtlMinutes = 15;
    private long refreshTokenTtlDays = 7;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenTtlMinutes() { return accessTokenTtlMinutes; }
    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) { this.accessTokenTtlMinutes = accessTokenTtlMinutes; }
    public long getRefreshTokenTtlDays() { return refreshTokenTtlDays; }
    public void setRefreshTokenTtlDays(long refreshTokenTtlDays) { this.refreshTokenTtlDays = refreshTokenTtlDays; }
}
