package com.foody.users.service;

import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.util.List;
import java.util.Optional;

/**
 * Public service contract for the users module.
 * Other modules (auth, businesses, ...) MUST depend only on this interface,
 * never on {@code UserRepository} directly.
 */
public interface UserService {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User create(User user);

    User save(User user);

    // Admin dashboard summary.
    long count();

    // Admin panel: user list, optionally filtered by role and/or status (either may be null).
    List<User> findAll(UserRole roleFilter, UserStatus statusFilter);

    // Admin panel: suspend/reactivate a user account. Refuses to change an ADMIN account's status.
    User updateStatus(Long userId, UserStatus newStatus);
}
