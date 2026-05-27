package com.authservice.security;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JWT Token Claims
 * Requirement: AUTH-FR-002, AUTH-FR-003
 */
@Data
public class TokenClaims {
    private UUID sub;  // Subject (user ID)
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private String iss;  // Issuer
    private String aud;  // Audience
    private Instant iat;  // Issued at
    private Instant exp;  // Expiration
    private UUID jti;  // JWT ID (unique token identifier)

    public boolean isExpired() {
        return Instant.now().isAfter(exp);
    }
}

