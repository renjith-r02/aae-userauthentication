package com.authservice.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ServiceException {
    public ConflictException(String message) {
        super(message, "CONFLICT_001", HttpStatus.CONFLICT);
    }
}
