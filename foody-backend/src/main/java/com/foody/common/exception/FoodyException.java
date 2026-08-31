package com.foody.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for all intentional, application-level errors.
 * Carries an HTTP status + a stable machine-readable error code so clients
 * (including the future frontend) can branch on {@code code} rather than text.
 */
public abstract class FoodyException extends RuntimeException {

    protected FoodyException(String message) {
        super(message);
    }

    public abstract HttpStatus getStatus();

    /** Stable, snake_case error code, e.g. USER_ALREADY_EXISTS. */
    public abstract String getCode();
}
