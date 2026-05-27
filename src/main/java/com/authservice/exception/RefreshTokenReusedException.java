package com.authservice.exception;

import java.util.UUID;

public class RefreshTokenReusedException extends AuthenticationException {
    public RefreshTokenReusedException(UUID sessionId) {
        super("Refresh token has already been used. Session: " + sessionId, "AUTH_007");
    }
}
