package com.mybanking.app.common.error;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final List<String> details;

    public AppException(ErrorCode errorCode, HttpStatus status, String message) {
        this(errorCode, status, message, List.of(), null);
    }

    public AppException(ErrorCode errorCode, HttpStatus status, String message, List<String> details) {
        this(errorCode, status, message, details, null);
    }

    public AppException(ErrorCode errorCode, HttpStatus status, String message, Throwable cause) {
        this(errorCode, status, message, List.of(), cause);
    }

    public AppException(ErrorCode errorCode, HttpStatus status, String message, List<String> details, Throwable cause) {
        super(Objects.requireNonNullElse(message, errorCode.name()), cause);
        this.status = Objects.requireNonNull(status);
        this.errorCode = Objects.requireNonNull(errorCode);
        this.details = (details == null) ? List.of() : List.copyOf(details);
    }

    public HttpStatus getStatus() { return status; }
    public ErrorCode getErrorCode() { return errorCode; }
    public List<String> getDetails() { return details; }

    public static AppException badRequest(ErrorCode code, String message) {
        return new AppException(code, HttpStatus.BAD_REQUEST, message);
    }
    public static AppException notFound(ErrorCode code, String message) {
        return new AppException(code, HttpStatus.NOT_FOUND, message);
    }
    public static AppException conflict(ErrorCode code, String message) {
        return new AppException(code, HttpStatus.CONFLICT, message);
    }
    public static AppException forbidden(ErrorCode code, String message) {
        return new AppException(code, HttpStatus.FORBIDDEN, message);
    }
}
