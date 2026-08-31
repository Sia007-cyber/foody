package com.foody.users.service;

import com.foody.users.entity.User;
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
}
