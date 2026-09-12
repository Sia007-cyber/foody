package com.foody.users.service;

import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.common.storage.ImageReplacement;
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

    Optional<User> findByPhone(String phone);

    List<User> findAllById(Iterable<Long> ids);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    User create(User user);

    User save(User user);

    ImageReplacement<User> replaceProfileImage(Long userId, String imageUrl);

    // Admin dashboard summary.
    long count();

    // Admin panel: user list, optionally filtered by role and/or status (either may be null).
    List<User> findAll(UserRole roleFilter, UserStatus statusFilter);

    // Admin panel: suspend/reactivate a user account. Refuses to change an ADMIN account's status.
    User updateStatus(Long userId, UserStatus newStatus);
}
