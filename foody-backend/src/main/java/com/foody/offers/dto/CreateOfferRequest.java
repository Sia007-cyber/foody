package com.foody.offers.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateOfferRequest(
        @NotBlank @Size(max=255) String title,
        @Size(max=2000) String description,
        @Min(1) int capacity,
        @NotNull Instant startsAt,
        @NotNull Instant expiresAt) {}
