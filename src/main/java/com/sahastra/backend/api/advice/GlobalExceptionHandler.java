package com.sahastra.backend.api.advice;

import com.sahastra.backend.api.dto.ErrorResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.exception.AccessDeniedException;
import com.sahastra.backend.exception.ApplicationException;
import com.sahastra.backend.exception.BusinessException;
import com.sahastra.backend.exception.ResourceNotFoundException;
import com.sahastra.backend.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST endpoints.
 * Provides consistent error response format with correlation IDs.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private String getCorrelationId() {
        return CorrelationIdContext.getCorrelationId();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .details(ex.getDetails())
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .details(ex.getDetails())
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .details(ex.getDetails())
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        log.warn("Business logic error: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .details(ex.getDetails())
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException ex, WebRequest request) {
        log.error("Application error: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .details(ex.getDetails())
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error in request body");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .message("Request validation failed")
                        .details(errors)
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException ex, WebRequest request) {
        HttpStatus status = hasCause(ex, MaxUploadSizeExceededException.class)
                ? HttpStatus.PAYLOAD_TOO_LARGE
                : HttpStatus.BAD_REQUEST;
        String code = status == HttpStatus.PAYLOAD_TOO_LARGE ? "UPLOAD_TOO_LARGE" : "INVALID_MULTIPART_REQUEST";
        String message = status == HttpStatus.PAYLOAD_TOO_LARGE
                ? "Uploaded file exceeds the maximum allowed size"
                : "Multipart request is invalid";
        log.warn("Multipart request rejected: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message("An unexpected error occurred. Please contact support.")
                        .correlationId(getCorrelationId())
                        .build())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> causeType) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (causeType.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }
}
