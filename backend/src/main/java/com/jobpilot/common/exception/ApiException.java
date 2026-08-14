package com.jobpilot.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base API exception carrying a doc 05 §12 error code + HTTP status.
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
