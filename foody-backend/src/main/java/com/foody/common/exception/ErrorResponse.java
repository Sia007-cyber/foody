package com.foody.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error envelope returned by every Foody API endpoint.
 * Never leak stack traces or internal exception messages to clients.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details) {

    public static ErrorResponse of(int status, String error, String code,
                                   String message, String path, List<String> details) {
        return new ErrorResponse(Instant.now(), status, error, code, message, path, details);
    }
}
