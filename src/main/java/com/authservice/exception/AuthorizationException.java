package com.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthorizationException extends ServiceException {
    private final String requiredPermission;

    public AuthorizationException(String permission) {
        super("Insufficient permissions. Required: " + permission, "AUTHZ_001", HttpStatus.FORBIDDEN);
        this.requiredPermission = permission;
    }
}
