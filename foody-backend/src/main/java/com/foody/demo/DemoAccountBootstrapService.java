package com.foody.demo;

import com.foody.auth.repository.RefreshTokenSessionRepository;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-enables only the historical V2/V5 records in an explicitly configured demo
 * deployment. The default path keeps the V19 disabled baseline intact.
 */
@Service
public class DemoAccountBootstrapService {
    static final long OWNER_ID = 1L;
    static final long ADMIN_ID = 2L;
    static final long BUSINESS_ID = 1L;
    static final String OWNER_EMAIL = "owner@foody.test";
    static final String ADMIN_EMAIL = "admin@foody.test";
    private static final String DISABLED_HASH = "!disabled-demo-account!";
    private static final Logger log = LoggerFactory.getLogger(DemoAccountBootstrapService.class);

    private final DemoAccountsProperties properties;
    private final UserRepository users;
    private final BusinessRepository businesses;
    private final RefreshTokenSessionRepository sessions;
    private final PasswordEncoder passwords;

    public DemoAccountBootstrapService(DemoAccountsProperties properties, UserRepository users,
                                       BusinessRepository businesses, RefreshTokenSessionRepository sessions,
                                       PasswordEncoder passwords) {
        this.properties = properties;
        this.users = users;
        this.businesses = businesses;
        this.sessions = sessions;
        this.passwords = passwords;
    }

    @Transactional
    public void apply() {
        if (!properties.isEnabled() || !hasRequiredPasswords()) {
            if (properties.isEnabled()) {
                log.warn("Demo accounts were requested but required password configuration is missing; keeping them disabled");
            }
            disableKnownDemoAccounts();
            return;
        }

        User owner = expectedUser(OWNER_ID, OWNER_EMAIL, UserRole.BUSINESS_OWNER);
        User admin = expectedUser(ADMIN_ID, ADMIN_EMAIL, UserRole.ADMIN);
        Business business = expectedBusiness(owner);
        if (owner == null || admin == null || business == null) {
            log.error("Demo account bootstrap refused because the expected V2/V5 seed identities do not match");
            disableKnownDemoAccounts();
            return;
        }

        activate(owner, properties.getOwnerPassword());
        activate(admin, properties.getAdminPassword());
        if (business.getStatus() != BusinessStatus.APPROVED) {
            business.setStatus(BusinessStatus.APPROVED);
            businesses.save(business);
        }
        log.info("Temporary demo accounts are enabled for this explicitly configured deployment");
    }

    private boolean hasRequiredPasswords() {
        return present(properties.getOwnerPassword()) && present(properties.getAdminPassword());
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }

    private User expectedUser(long id, String email, UserRole role) {
        return users.findById(id)
                .filter(user -> email.equals(user.getEmail()) && role == user.getRole())
                .orElse(null);
    }

    private Business expectedBusiness(User owner) {
        if (owner == null) return null;
        return businesses.findById(BUSINESS_ID)
                .filter(business -> OWNER_ID == business.getOwnerUserId()
                        && "کافه سان‌رایز".equals(business.getName()))
                .orElse(null);
    }

    private void activate(User user, String password) {
        boolean passwordChanged = !passwords.matches(password, user.getPasswordHash());
        boolean statusChanged = user.getStatus() != UserStatus.ACTIVE;
        if (passwordChanged || statusChanged) {
            user.setPasswordHash(passwords.encode(password));
            user.setStatus(UserStatus.ACTIVE);
            users.save(user);
            sessions.revokeAllActiveByUserId(user.getId());
        }
    }

    private void disableKnownDemoAccounts() {
        disable(expectedUser(OWNER_ID, OWNER_EMAIL, UserRole.BUSINESS_OWNER));
        disable(expectedUser(ADMIN_ID, ADMIN_EMAIL, UserRole.ADMIN));
        businesses.findById(BUSINESS_ID)
                .filter(business -> OWNER_ID == business.getOwnerUserId()
                        && "کافه سان‌رایز".equals(business.getName()))
                .ifPresent(business -> {
                    if (business.getStatus() != BusinessStatus.SUSPENDED) {
                        business.setStatus(BusinessStatus.SUSPENDED);
                        businesses.save(business);
                    }
                });
    }

    private void disable(User user) {
        if (user == null) return;
        if (user.getStatus() != UserStatus.SUSPENDED || !DISABLED_HASH.equals(user.getPasswordHash())) {
            user.setStatus(UserStatus.SUSPENDED);
            user.setPasswordHash(DISABLED_HASH);
            users.save(user);
        }
        sessions.revokeAllActiveByUserId(user.getId());
    }
}
