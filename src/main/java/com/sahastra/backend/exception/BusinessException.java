package com.sahastra.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for business logic errors (e.g., insufficient stock, invalid state).
 */
public class BusinessException extends ApplicationException {
    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(String errorCode, String message, Object details) {
        super(errorCode, message, details);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
