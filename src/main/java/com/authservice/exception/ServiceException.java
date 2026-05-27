package com.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String traceId;

    protected ServiceException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.traceId = java.util.UUID.randomUUID().toString();
    }
}
