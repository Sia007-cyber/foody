package com.foody.admin.provisioning;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Values are supplied only to the short-lived, non-web provisioning process. */
@Component
@ConfigurationProperties(prefix = "foody.primary-admin-provision")
public class PrimaryAdminProvisioningProperties {
    private boolean enabled;
    private String email;
    private String phone;
    private String fullName;
    private String password;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
