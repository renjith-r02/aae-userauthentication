package com.authservice.exception;

public class TokenInvalidException extends AuthenticationException {
    public TokenInvalidException(String reason) {
        super("Invalid token: " + reason, "AUTH_003");
    }
}
