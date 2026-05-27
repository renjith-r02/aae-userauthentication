package com.authservice.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * JWT Configuration Properties
 * Requirement: AUTH-FR-002 (JWT Configuration)
 * Section 13: Configuration Requirements
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JWTConfig {
    private String issuer = "auth-service";
    private String audience = "application-api";
    private Duration accessTokenExpiry = Duration.ofMinutes(15);
    private Duration refreshTokenExpiry = Duration.ofDays(30);
    private String algorithm = "RS256";
}

