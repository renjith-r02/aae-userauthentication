package com.authservice.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * Password Configuration
 * Requirement: AUTH-FR-001
 */
@Configuration
@ConfigurationProperties(prefix = "password")
@Data
public class PasswordConfig {
    private int bcryptStrength = 12;
    private PolicyConfig policy = new PolicyConfig();
    @Data
    public static class PolicyConfig {
        private int minLength = 12;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecialChar = true;
    }
}
