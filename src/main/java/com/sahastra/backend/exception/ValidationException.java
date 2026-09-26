package com.sahastra.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for validation errors.
 */
public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    public ValidationException(String message, Object details) {
        super("VALIDATION_ERROR", message, details);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
