package com.sahastra.backend.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for exception classes.
 */
class ExceptionTest {

    @Test
    void testApplicationExceptionWithCodeAndMessage() {
        ApplicationException ex = new ApplicationException("TEST_ERROR", "Test message");
        
        assertEquals("TEST_ERROR", ex.getErrorCode());
        assertEquals("Test message", ex.getMessage());
        assertNull(ex.getDetails());
    }

    @Test
    void testApplicationExceptionWithDetails() {
        String details = "Additional details";
        ApplicationException ex = new ApplicationException("TEST_ERROR", "Test message", details);
        
        assertEquals("TEST_ERROR", ex.getErrorCode());
        assertEquals("Test message", ex.getMessage());
        assertEquals(details, ex.getDetails());
    }

    @Test
    void testResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "123");
        
        assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
        assertEquals("User not found: 123", ex.getMessage());
    }

    @Test
    void testAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Insufficient permissions");
        
        assertEquals("ACCESS_DENIED", ex.getErrorCode());
        assertEquals("Insufficient permissions", ex.getMessage());
    }

    @Test
    void testValidationException() {
        ValidationException ex = new ValidationException("Invalid email format");
        
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
        assertEquals("Invalid email format", ex.getMessage());
    }

    @Test
    void testBusinessException() {
        BusinessException ex = new BusinessException("INSUFFICIENT_STOCK", "Not enough stock available");
        
        assertEquals("INSUFFICIENT_STOCK", ex.getErrorCode());
        assertEquals("Not enough stock available", ex.getMessage());
    }
}
