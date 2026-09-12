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
import com.foody.auth.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.foody.common.storage.ImageUploadService;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final ImageUploadService imageUploadService;

    public UsersController(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthService authService, ImageUploadService imageUploadService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse replaceProfileImage(@AuthenticationPrincipal FoodyUserPrincipal principal,
                                            @RequestParam("file") MultipartFile file) {
        var upload = imageUploadService.store(ImageUploadService.UploadCategory.PROFILE, file);
        try {
            var replacement = userService.replaceProfileImage(principal.getUserId(), upload.publicUrl());
            deletePreviousAfterSuccess(replacement.previousUrl());
            return UserResponse.from(replacement.value());
        } catch (RuntimeException ex) {
            cleanupFailedUpload(upload.publicUrl(), ex);
            throw ex;
        }
    }

    private void deletePreviousAfterSuccess(String previousUrl) {
        try { imageUploadService.deleteManagedUrl(previousUrl); }
        catch (RuntimeException ignored) { /* replacement is committed; stale cleanup can be retried operationally */ }
    }

    private void cleanupFailedUpload(String publicUrl, RuntimeException original) {
        try { imageUploadService.deleteManagedUrl(publicUrl); }
        catch (RuntimeException cleanup) { original.addSuppressed(cleanup); }
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal FoodyUserPrincipal principal) {
        User user = principal.getUser();
        return UserResponse.from(user);
    }

    @PatchMapping("/me")
    @Transactional
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
            String phone = request.phone().trim();
            if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
                throw new DuplicateResourceException("این شماره موبایل قبلاً استفاده شده");
            }
            user.setPhone(phone);
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
            throw new com.foody.common.exception.InvalidRequestException(
                    "Use /api/users/me/profile-image to replace the profile image");
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            authService.revokeAllRefreshSessions(user.getId());
        }
        user = userService.save(user);
        return UserResponse.from(user);
    }
}
