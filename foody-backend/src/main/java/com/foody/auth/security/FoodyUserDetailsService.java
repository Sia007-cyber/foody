package com.foody.auth.security;

import com.foody.common.exception.InvalidCredentialsException;
import com.foody.users.entity.User;
import com.foody.users.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Loads a FoodyUserPrincipal by email. Depends ONLY on the users module's
 * UserService interface — never on its repository — preserving module boundaries.
 */
@Component
public class FoodyUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public FoodyUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new FoodyUserPrincipal(user);
    }

    public UserDetails loadByUserId(Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid account"));
        return new FoodyUserPrincipal(user);
    }
}
