package com.sahastra.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for resource not found errors.
 */
public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resourceType, String identifier) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s not found: %s", resourceType, identifier));
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
