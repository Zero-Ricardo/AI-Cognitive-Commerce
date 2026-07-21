package com.aishop.commerce.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }

    public static BusinessException notFound(String message) {
        return new BusinessException("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }
}
