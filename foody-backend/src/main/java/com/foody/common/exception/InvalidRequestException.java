package com.foody.common.exception;

import org.springframework.http.HttpStatus;

/** Raised when a request violates a business rule (e.g. empty cart, unavailable product). */
public class InvalidRequestException extends FoodyException {

    public InvalidRequestException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String getCode() {
        return "INVALID_REQUEST";
    }
}
