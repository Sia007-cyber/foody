package com.foody.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @JsonAlias("email") String identifier,
        @NotBlank String password) {
}
