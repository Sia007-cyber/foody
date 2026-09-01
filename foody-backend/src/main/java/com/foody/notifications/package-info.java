/**
 * Foody notifications module (Phase 1: in-app only).
 *
 * <p>Other modules (orders, reservations, businesses) create notifications by depending on
 * {@link com.foody.notifications.service.NotificationService} — never on the entity or
 * repository directly. Delivery is in-app storage only; real push/SMS/email delivery is
 * deferred to a later phase (see decision log), but the service contract is written so
 * that a future delivery channel can be added without changing callers.
 */
package com.foody.notifications;
