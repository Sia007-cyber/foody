package com.foody.common.exception;

import org.springframework.http.HttpStatus;

/** Raised when an action is attempted on a resource that is not in a valid state for it
 *  (e.g. cancelling an order that has already been accepted by the business). */
public class InvalidStateTransitionException extends FoodyException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getCode() {
        return "INVALID_STATE_TRANSITION";
    }
}
