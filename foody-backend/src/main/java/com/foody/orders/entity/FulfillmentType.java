package com.foody.orders.entity;

/** How the customer will receive the order. Dine-in has no food-order flow yet — see reservations module. */
public enum FulfillmentType {
    PICKUP,
    DELIVERY
}
