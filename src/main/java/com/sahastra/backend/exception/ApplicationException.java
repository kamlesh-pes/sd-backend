package com.sahastra.backend.exception;

/**
 * Base exception for all application-specific errors.
 */
public class ApplicationException extends RuntimeException {
    private final String errorCode;
    private final Object details;

    public ApplicationException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApplicationException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ApplicationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApplicationException(String errorCode, String message, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}
