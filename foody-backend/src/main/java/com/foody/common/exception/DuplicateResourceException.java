package com.foody.common.exception;

import org.springframework.http.HttpStatus;

/** Raised when a unique constraint would be violated (e.g. duplicate email). */
public class DuplicateResourceException extends FoodyException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getCode() {
        return "RESOURCE_ALREADY_EXISTS";
    }
}
