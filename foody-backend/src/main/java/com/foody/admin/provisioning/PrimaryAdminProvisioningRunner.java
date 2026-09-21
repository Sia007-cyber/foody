package com.foody.admin.provisioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** One-shot CLI runner. It refuses to execute while the HTTP server is enabled. */
@Component
@Order(100)
@ConditionalOnProperty(name = "foody.primary-admin-provision.enabled", havingValue = "true")
public class PrimaryAdminProvisioningRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PrimaryAdminProvisioningRunner.class);
    private final PrimaryAdminProvisioningProperties properties;
    private final PrimaryAdminProvisioningService provisioning;
    private final Environment environment;

    public PrimaryAdminProvisioningRunner(PrimaryAdminProvisioningProperties properties,
            PrimaryAdminProvisioningService provisioning, Environment environment) {
        this.properties = properties;
        this.provisioning = provisioning;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String applicationType = environment.getProperty("spring.main.web-application-type", "servlet");
        if (!"none".equalsIgnoreCase(applicationType)) {
            throw new IllegalStateException("Primary-admin provisioning requires spring.main.web-application-type=none");
        }
        PrimaryAdminProvisioningService.Outcome outcome = provisioning.provision(properties);
        log.info("Primary-admin provisioning completed: {}", outcome);
    }
}
