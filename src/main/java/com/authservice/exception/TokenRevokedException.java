package com.authservice.exception;

public class TokenRevokedException extends AuthenticationException {
    public TokenRevokedException() {
        super("Token has been revoked", "AUTH_005");
    }
}
