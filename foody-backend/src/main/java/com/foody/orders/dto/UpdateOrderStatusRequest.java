package com.foody.orders.dto;

import com.foody.orders.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "status is required")
        OrderStatus status
) {
}
