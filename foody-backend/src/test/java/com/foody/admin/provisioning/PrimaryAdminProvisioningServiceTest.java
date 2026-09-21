package com.foody.admin.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.admin.entity.AdminRoleAudit;
import com.foody.admin.repository.AdminRoleAuditRepository;
import com.foody.auth.dto.RegisterRequest;
import com.foody.auth.service.RoleChangeSessionService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PrimaryAdminProvisioningServiceTest {
    @Mock UserRepository users;
    @Mock AdminRoleAuditRepository audits;
    @Mock RoleChangeSessionService sessions;
    @Mock PasswordEncoder passwords;
    @Mock Validator validator;

    PrimaryAdminProvisioningService service;
    PrimaryAdminProvisioningProperties properties;

    @BeforeEach
    void setUp() {
        service = new PrimaryAdminProvisioningService(users, audits, sessions, passwords, validator);
        properties = new PrimaryAdminProvisioningProperties();
        properties.setEmail(" primary@example.test ");
        properties.setPhone(" 09120000000 ");
        properties.setFullName("Primary Operator");
        properties.setPassword("strongPassword123");
        when(validator.validate(any(RegisterRequest.class))).thenReturn(Set.of());
    }

    @Test
    void createsPrimaryWithBcryptAndAuditWhenIdentityIsUnused() {
        when(users.findPrimaryAdminForUpdate()).thenReturn(Optional.empty());
        when(users.findByEmail("primary@example.test")).thenReturn(Optional.empty());
        when(users.findByPhone("09120000000")).thenReturn(Optional.empty());
        when(passwords.encode("strongPassword123")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        assertThat(service.provision(properties)).isEqualTo(PrimaryAdminProvisioningService.Outcome.CREATED);
        verify(users).saveAndFlush(org.mockito.ArgumentMatchers.argThat(user -> user.isPrimaryAdmin()
                && user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ACTIVE
                && "bcrypt-hash".equals(user.getPasswordHash())));
        verify(audits).save(any(AdminRoleAudit.class));
        verify(sessions).invalidate(42L);
    }

    @Test
    void sameExistingPrimaryIsIdempotent() {
        User current = account(7L, "primary@example.test", "09120000000");
        current.setPrimaryAdmin(true);
        when(users.findPrimaryAdminForUpdate()).thenReturn(Optional.of(current));

        assertThat(service.provision(properties)).isEqualTo(PrimaryAdminProvisioningService.Outcome.ALREADY_CONFIGURED);
        verify(passwords, never()).encode(any());
        verify(audits, never()).save(any());
    }

    @Test
    void differentExistingPrimaryFailsClosed() {
        User current = account(7L, "different@example.test", "09121111111");
        current.setPrimaryAdmin(true);
        when(users.findPrimaryAdminForUpdate()).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.provision(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different primary admin");
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void splitEmailAndPhoneIdentityFailsClosed() {
        when(users.findPrimaryAdminForUpdate()).thenReturn(Optional.empty());
        when(users.findByEmail("primary@example.test"))
                .thenReturn(Optional.of(account(1L, "primary@example.test", "09123333333")));
        when(users.findByPhone("09120000000"))
                .thenReturn(Optional.of(account(2L, "other@example.test", "09120000000")));

        assertThatThrownBy(() -> service.provision(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conflicting accounts");
        verify(users, never()).saveAndFlush(any());
    }

    private User account(Long id, String email, String phone) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName("Existing Account");
        user.setPasswordHash("existing-hash");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
