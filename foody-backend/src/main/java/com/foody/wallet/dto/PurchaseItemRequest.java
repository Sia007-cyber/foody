package com.foody.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseItemRequest(@NotNull Long productId, @Min(1) int quantity) {}
