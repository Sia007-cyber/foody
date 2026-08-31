package com.foody.users.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.users.dto.UpdateProfileRequest;
import com.foody.users.dto.UserResponse;
import com.foody.users.entity.User;
import com.foody.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UsersController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        User user = principal.getUser();
        return UserResponse.from(user);
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user = userService.save(user);
        return UserResponse.from(user);
    }
}
