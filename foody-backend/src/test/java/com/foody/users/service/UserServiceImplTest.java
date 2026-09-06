package com.foody.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;

    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    private User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("customer@foody.test");
        user.setPhone("0912xxxxxxx");
        user.setPasswordHash("hashed");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void findById_returnsUserWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser()));

        Optional<User> result = userService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("customer@foody.test");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_returnsUserWhenFound() {
        when(userRepository.findByEmail("customer@foody.test")).thenReturn(Optional.of(sampleUser()));

        Optional<User> result = userService.findByEmail("customer@foody.test");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByEmail_returnsEmptyWhenNotFound() {
        when(userRepository.findByEmail("nobody@foody.test")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nobody@foody.test");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail_delegatesToRepository() {
        when(userRepository.existsByEmail("customer@foody.test")).thenReturn(true);

        boolean result = userService.existsByEmail("customer@foody.test");

        assertThat(result).isTrue();
    }

    @Test
    void existsByEmail_returnsFalseWhenAbsent() {
        when(userRepository.existsByEmail("nobody@foody.test")).thenReturn(false);

        boolean result = userService.existsByEmail("nobody@foody.test");

        assertThat(result).isFalse();
    }

    @Test
    void create_savesAndReturnsUser() {
        User newUser = sampleUser();
        newUser.setId(null);

        when(userRepository.save(newUser)).thenReturn(sampleUser());

        User result = userService.create(newUser);

        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).save(newUser);
    }

    @Test
    void save_persistsChangesAndReturnsUpdatedUser() {
        User updated = sampleUser();
        updated.setFullName("Renamed Customer");

        when(userRepository.save(updated)).thenReturn(updated);

        User result = userService.save(updated);

        assertThat(result.getFullName()).isEqualTo("Renamed Customer");
        verify(userRepository).save(updated);
    }

    @Test
    void count_delegatesToRepository() {
        when(userRepository.count()).thenReturn(42L);

        long result = userService.count();

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void findAll_delegatesToRepositorySearchWithFilters() {
        when(userRepository.search(UserRole.CUSTOMER, UserStatus.ACTIVE)).thenReturn(List.of(sampleUser()));

        List<User> result = userService.findAll(UserRole.CUSTOMER, UserStatus.ACTIVE);

        assertThat(result).hasSize(1);
        verify(userRepository).search(UserRole.CUSTOMER, UserStatus.ACTIVE);
    }

    @Test
    void updateStatus_suspendsACustomerAccount() {
        User active = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        when(userRepository.save(active)).thenReturn(active);

        User result = userService.updateStatus(1L, UserStatus.SUSPENDED);

        assertThat(result.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void updateStatus_reactivatesASuspendedAccount() {
        User suspended = sampleUser();
        suspended.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(suspended));
        when(userRepository.save(suspended)).thenReturn(suspended);

        User result = userService.updateStatus(1L, UserStatus.ACTIVE);

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateStatus_rejectsChangingAnAdminAccount() {
        User admin = sampleUser();
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.updateStatus(1L, UserStatus.SUSPENDED))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateStatus_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateStatus(99L, UserStatus.SUSPENDED))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
