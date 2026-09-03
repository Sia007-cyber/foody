package com.foody.users.controller;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.DuplicateResourceException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.users.dto.UpdateProfileRequest;
import com.foody.users.dto.UserResponse;
import com.foody.users.entity.User;
import com.foody.users.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UsersController(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
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
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("این ایمیل قبلاً استفاده شده");
            }
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.latitude() != null) {
            user.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            user.setLongitude(request.longitude());
        }
        if (request.profileImageUrl() != null) {
            user.setProfileImageUrl(request.profileImageUrl());
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user = userService.save(user);
        return UserResponse.from(user);
    }
}
