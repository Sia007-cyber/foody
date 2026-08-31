package com.foody.common.exception;

import org.springframework.http.HttpStatus;

/** Raised when an expected entity does not exist. */
public class ResourceNotFoundException extends FoodyException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getCode() {
        return "RESOURCE_NOT_FOUND";
    }
}
