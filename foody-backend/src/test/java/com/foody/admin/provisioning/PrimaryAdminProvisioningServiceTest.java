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
        org.mockito.Mockito.lenient().when(passwords.encode("strongPassword123")).thenReturn("bcrypt-hash");
    }

    @Test
    void createsPrimaryWithBcryptAndAuditWhenIdentityIsUnused() {
        when(users.findPrimaryAdminForUpdate()).thenReturn(Optional.empty());
        when(users.findByEmail("primary@example.test")).thenReturn(Optional.empty());
        when(users.findByPhone("09120000000")).thenReturn(Optional.empty());
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
    void existingPhoneAndUnusedEmailPromotesAccountAndAttachesEmail() {
        User existing = account(4L, null, "09120000000", UserRole.BUSINESS_OWNER);
        arrangeExistingMatch(Optional.empty(), Optional.of(existing), existing);

        assertThat(service.provision(properties)).isEqualTo(PrimaryAdminProvisioningService.Outcome.PROMOTED);
        assertThat(existing.getEmail()).isEqualTo("primary@example.test");
        assertThat(existing.getPhone()).isEqualTo("09120000000");
        assertThat(existing.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(existing.getAdminBaseRole()).isEqualTo(UserRole.BUSINESS_OWNER);
        verify(audits).save(org.mockito.ArgumentMatchers.argThat(audit ->
                audit.getPreviousRole() == UserRole.BUSINESS_OWNER
                        && audit.getNewRole() == UserRole.ADMIN));
        verify(sessions).invalidate(4L);
    }

    @Test
    void existingEmailAndUnusedPhonePromotesAccountAndAttachesPhone() {
        User existing = account(5L, "primary@example.test", null, UserRole.CUSTOMER);
        arrangeExistingMatch(Optional.of(existing), Optional.empty(), existing);

        assertThat(service.provision(properties)).isEqualTo(PrimaryAdminProvisioningService.Outcome.PROMOTED);
        assertThat(existing.getEmail()).isEqualTo("primary@example.test");
        assertThat(existing.getPhone()).isEqualTo("09120000000");
        assertThat(existing.getAdminBaseRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void bothIdentitiesResolvingToSameUserPromoteThatUser() {
        User existing = account(6L, "primary@example.test", "09120000000", UserRole.BUSINESS_OWNER);
        arrangeExistingMatch(Optional.of(existing), Optional.of(existing), existing);

        assertThat(service.provision(properties)).isEqualTo(PrimaryAdminProvisioningService.Outcome.PROMOTED);
        assertThat(existing.isPrimaryAdmin()).isTrue();
        assertThat(existing.getAdminBaseRole()).isEqualTo(UserRole.BUSINESS_OWNER);
    }

    @Test
    void existingAccountWithDifferentNonMissingIdentityIsNotOverwritten() {
        User existing = account(6L, "existing@example.test", "09120000000", UserRole.BUSINESS_OWNER);
        arrangeExistingMatch(Optional.empty(), Optional.of(existing), existing);

        assertThatThrownBy(() -> service.provision(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer matches");
        assertThat(existing.getEmail()).isEqualTo("existing@example.test");
        verify(users, never()).saveAndFlush(any());
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
                .thenReturn(Optional.of(account(1L, "primary@example.test", "09123333333", UserRole.CUSTOMER)));
        when(users.findByPhone("09120000000"))
                .thenReturn(Optional.of(account(2L, "other@example.test", "09120000000", UserRole.BUSINESS_OWNER)));

        assertThatThrownBy(() -> service.provision(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conflicting accounts");
        verify(users, never()).saveAndFlush(any());
    }

    private void arrangeExistingMatch(Optional<User> byEmail, Optional<User> byPhone, User locked) {
        when(users.findPrimaryAdminForUpdate()).thenReturn(Optional.empty());
        when(users.findByEmail("primary@example.test")).thenReturn(byEmail);
        when(users.findByPhone("09120000000")).thenReturn(byPhone);
        when(users.findByIdForUpdate(locked.getId())).thenReturn(Optional.of(locked));
        org.mockito.Mockito.lenient().when(users.saveAndFlush(locked)).thenReturn(locked);
    }

    private User account(Long id, String email, String phone) {
        return account(id, email, phone, UserRole.ADMIN);
    }

    private User account(Long id, String email, String phone, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName("Existing Account");
        user.setPasswordHash("existing-hash");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
