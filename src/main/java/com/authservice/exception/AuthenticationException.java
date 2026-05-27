package com.authservice.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends ServiceException {
    public AuthenticationException(String message) {
        super(message, "AUTH_001", HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
}
