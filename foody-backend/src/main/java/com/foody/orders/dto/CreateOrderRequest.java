package com.foody.orders.dto;

import com.foody.orders.entity.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(

        @NotNull(message = "businessId is required")
        Long businessId,

        @NotNull(message = "fulfillmentType is required")
        FulfillmentType fulfillmentType,

        @NotEmpty(message = "order must contain at least one item")
        @Valid
        List<OrderItemRequest> items,

        // Required only when fulfillmentType is DELIVERY; validated in the service layer
        // since that rule depends on another field, not expressible with a simple annotation.
        String deliveryAddress
) {
}
