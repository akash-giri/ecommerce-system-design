package com.ecommerce.user.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .path(path)
                        .build()
        );
    }
}
