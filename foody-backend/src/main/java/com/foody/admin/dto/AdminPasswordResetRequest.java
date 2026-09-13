package com.foody.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordResetRequest(
        @NotBlank @Size(min = 8, max = 128) String newPassword) { }
