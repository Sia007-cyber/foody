package com.foody.common.exception;

import org.springframework.http.HttpStatus;

/** Raised when authentication fails (bad email/password) or a token is invalid/expired. */
public class InvalidCredentialsException extends FoodyException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String getCode() {
        return "INVALID_CREDENTIALS";
    }
}
