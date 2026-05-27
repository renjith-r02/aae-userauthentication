package com.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitExceededException extends ServiceException {
    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfter) {
        super("Rate limit exceeded. Please try again later", "RATE_LIMIT_001", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfter;
    }
}
