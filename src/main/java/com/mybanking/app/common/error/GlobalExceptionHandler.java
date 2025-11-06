package com.mybanking.app.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException ex, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                req.getRequestURI(),
                ex.getStatus().value(),
                ex.getErrorCode().name(),
                ex.getMessage(),
                ex.getDetails(),
                traceId()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                req.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed",
                details,
                traceId()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                req.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed",
                details,
                traceId()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                req.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_JSON",
                "Request body is invalid",
                List.of(Optional.ofNullable(ex.getMostSpecificCause()).map(Throwable::getMessage).orElse(ex.getMessage())),
                traceId()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception path={}", req.getRequestURI(), ex);
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                req.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Unexpected error",
                List.of(ex.getMessage()),
                traceId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private String traceId() {
        return UUID.randomUUID().toString();
    }
}
