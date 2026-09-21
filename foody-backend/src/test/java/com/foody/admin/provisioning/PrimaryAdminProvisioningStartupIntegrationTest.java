package com.foody.admin.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.foody.AbstractContainerBaseTest;
import com.foody.auth.config.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("tc")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.main.web-application-type=none",
        "foody.primary-admin-provision.enabled=true"
})
class PrimaryAdminProvisioningStartupIntegrationTest extends AbstractContainerBaseTest {
    @MockitoBean PrimaryAdminProvisioningService provisioning;
    @Autowired ApplicationContext applicationContext;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void nonWebProvisioningStartsWithoutServletSecurityAndExecutesRunner() {
        verify(provisioning).provision(any(PrimaryAdminProvisioningProperties.class));
        assertThat(applicationContext.getBeansOfType(WebSecurityConfig.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain.class)).isEmpty();
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }
}
