package com.ledgerline.backend.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static ApiException badRequest(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", msg);
    }
    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Account not found");
    }
    public static ApiException insufficientFunds() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS", "Insufficient funds");
    }
    public static ApiException accountNotActive() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_NOT_ACTIVE", "Account is not active");
    }
    public static ApiException idempotencyConflict() {
        return new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                "Idempotency key was already used with a different request");
    }
    public static ApiException conflict(String msg) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", msg);
    }
    public static ApiException unprocessable(String msg) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE", msg);
    }
    public static ApiException resourceNotFound(String msg) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", msg);
    }
}