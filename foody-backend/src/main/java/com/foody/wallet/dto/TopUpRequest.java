package com.foody.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TopUpRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "1000", message = "amount must be at least 1000 تومان")
        BigDecimal amount
) {
}
