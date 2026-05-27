package com.authservice.exception;

public class TokenExpiredException extends AuthenticationException {
    public TokenExpiredException() {
        super("Token has expired", "AUTH_004");
    }
}
