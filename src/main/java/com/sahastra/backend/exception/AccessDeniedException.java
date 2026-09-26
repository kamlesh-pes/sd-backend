package com.sahastra.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for access denied/authorization errors.
 */
public class AccessDeniedException extends ApplicationException {
    public AccessDeniedException(String message) {
        super("ACCESS_DENIED", message);
    }

    public AccessDeniedException(String message, Object details) {
        super("ACCESS_DENIED", message, details);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
