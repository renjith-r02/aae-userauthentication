package com.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limit Configuration
 * Requirement: AUTH-FR-002 (Rate limiting for auth endpoints)
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitConfig {
    
    private LoginConfig login = new LoginConfig();
    private RegistrationConfig registration = new RegistrationConfig();
    private RefreshConfig refresh = new RefreshConfig();
    private ApiConfig api = new ApiConfig();
    
    @Data
    public static class LoginConfig {
        private int maxAttempts = 5;
        private String window = "15m";
        
        public Duration getWindowDuration() {
            return parseDuration(window);
        }
    }
    
    @Data
    public static class RegistrationConfig {
        private int maxAttempts = 3;
        private String window = "1h";
        
        public Duration getWindowDuration() {
            return parseDuration(window);
        }
    }
    
    @Data
    public static class RefreshConfig {
        private int maxAttempts = 10;
        private String window = "5m";
        
        public Duration getWindowDuration() {
            return parseDuration(window);
        }
    }
    
    @Data
    public static class ApiConfig {
        private int maxAttempts = 100;
        private String window = "1m";
        
        public Duration getWindowDuration() {
            return parseDuration(window);
        }
    }
    
    private static Duration parseDuration(String duration) {
        if (duration.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(duration.substring(0, duration.length() - 1)));
        } else if (duration.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(duration.substring(0, duration.length() - 1)));
        } else if (duration.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(duration.substring(0, duration.length() - 1)));
        }
        return Duration.ofMinutes(15); // default
    }
}

