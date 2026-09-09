package com.foody.common.exception;

import org.springframework.http.HttpStatus;

public class StorageOperationException extends FoodyException {
    public StorageOperationException(String message, Throwable cause) { super(message); initCause(cause); }
    @Override public HttpStatus getStatus() { return HttpStatus.SERVICE_UNAVAILABLE; }
    @Override public String getCode() { return "STORAGE_UNAVAILABLE"; }
}
