package com.foody.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Explicit, off-by-default configuration for the temporary client-demo environment. */
@Component
@ConfigurationProperties(prefix = "foody.demo-accounts")
public class DemoAccountsProperties {
    private boolean enabled;
    private String ownerPassword;
    private String adminPassword;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getOwnerPassword() { return ownerPassword; }
    public void setOwnerPassword(String ownerPassword) { this.ownerPassword = ownerPassword; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
}
