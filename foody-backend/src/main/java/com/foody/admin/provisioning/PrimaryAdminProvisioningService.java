package com.foody.admin.provisioning;

import com.foody.admin.entity.AdminRoleAction;
import com.foody.admin.entity.AdminRoleAudit;
import com.foody.admin.repository.AdminRoleAuditRepository;
import com.foody.auth.dto.RegisterRequest;
import com.foody.auth.service.RoleChangeSessionService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional, fail-closed provisioning used only by the non-web runner. */
@Service
public class PrimaryAdminProvisioningService {
    public enum Outcome { CREATED, PROMOTED, ALREADY_CONFIGURED }

    private final UserRepository users;
    private final AdminRoleAuditRepository audits;
    private final RoleChangeSessionService sessions;
    private final PasswordEncoder passwords;
    private final Validator validator;

    public PrimaryAdminProvisioningService(UserRepository users, AdminRoleAuditRepository audits,
            RoleChangeSessionService sessions, PasswordEncoder passwords, Validator validator) {
        this.users = users;
        this.audits = audits;
        this.sessions = sessions;
        this.passwords = passwords;
        this.validator = validator;
    }

    @Transactional
    public Outcome provision(PrimaryAdminProvisioningProperties properties) {
        String email = normalizeEmail(properties.getEmail());
        String phone = normalizePhone(properties.getPhone());
        String fullName = properties.getFullName() == null ? null : properties.getFullName().trim();
        String password = properties.getPassword();
        validateInputs(email, phone, fullName, password);

        Optional<User> currentPrimary = users.findPrimaryAdminForUpdate();
        if (currentPrimary.isPresent()) {
            User current = currentPrimary.get();
            if (email.equals(current.getEmail()) && phone.equals(current.getPhone())) {
                return Outcome.ALREADY_CONFIGURED;
            }
            throw new IllegalStateException("A different primary admin already exists");
        }

        Optional<User> byEmail = users.findByEmail(email);
        Optional<User> byPhone = users.findByPhone(phone);
        if (byEmail.isPresent() != byPhone.isPresent()
                || (byEmail.isPresent() && !byEmail.get().getId().equals(byPhone.orElseThrow().getId()))) {
            throw new IllegalStateException("The supplied email and phone belong to conflicting accounts");
        }

        boolean created = byEmail.isEmpty();
        User target;
        UserRole previousRole = null;
        if (created) {
            target = new User();
            target.setEmail(email);
            target.setPhone(phone);
            target.setFullName(fullName);
        } else {
            target = users.findByIdForUpdate(byEmail.orElseThrow().getId())
                    .orElseThrow(() -> new IllegalStateException("The matching account disappeared during provisioning"));
            if (!email.equals(target.getEmail()) || !phone.equals(target.getPhone())) {
                throw new IllegalStateException("The supplied identity no longer matches the account");
            }
            previousRole = target.getRole();
        }

        target.setPasswordHash(passwords.encode(password));
        target.setRole(UserRole.ADMIN);
        target.setAdminBaseRole(null);
        target.setPrimaryAdmin(true);
        target.setStatus(UserStatus.ACTIVE);
        User saved = users.saveAndFlush(target);
        audits.save(new AdminRoleAudit(saved, saved, previousRole, UserRole.ADMIN,
                AdminRoleAction.PROVISION_PRIMARY_ADMIN));
        sessions.invalidate(saved.getId());
        return created ? Outcome.CREATED : Outcome.PROMOTED;
    }

    private void validateInputs(String email, String phone, String fullName, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Invalid primary-admin provisioning fields: email");
        }
        RegisterRequest candidate = new RegisterRequest(email, password, fullName, phone, UserRole.CUSTOMER);
        var violations = validator.validate(candidate);
        if (!violations.isEmpty()) {
            String fields = violations.stream()
                    .map(ConstraintViolation::getPropertyPath)
                    .map(Object::toString)
                    .sorted()
                    .distinct()
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid primary-admin provisioning fields: " + fields);
        }
    }

    private static String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String value) {
        return value == null ? null : value.trim();
    }
}
