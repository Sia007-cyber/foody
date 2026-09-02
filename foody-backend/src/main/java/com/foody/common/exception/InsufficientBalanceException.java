package com.foody.common.exception;

import org.springframework.http.HttpStatus;

/** Raised when a wallet debit is attempted but the wallet's balance is too low to cover it. */
public class InsufficientBalanceException extends FoodyException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getCode() {
        return "INSUFFICIENT_BALANCE";
    }
}
