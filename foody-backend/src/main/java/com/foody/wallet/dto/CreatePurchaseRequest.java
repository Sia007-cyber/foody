package com.foody.wallet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreatePurchaseRequest(@NotEmpty List<@Valid PurchaseItemRequest> items) {}
