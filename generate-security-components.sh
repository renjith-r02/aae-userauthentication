#!/bin/bash
BASE="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/src/main/java/com/authservice"
echo "🔐 Creating Security Components..."
# JWTManager
cat > "$BASE/security/JWTManager.java" << 'EOF'
package com.authservice.security;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.authservice.exception.TokenExpiredException;
import com.authservice.exception.TokenInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
/**
 * JWT Manager for RS256 token generation and validation
 * Requirement: AUTH-FR-002, AUTH-FR-003
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JWTManager {
    private final JWTConfig jwtConfig;
    // For demo purposes - in production, load from AWS Secrets Manager
    private static final String PRIVATE_KEY_PEM = 
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKj" +
        "MzEfYyjiWA4R4/M2bS1+fWIcPm15A8vMp2zvyBz8Q6zfZ8xFzQY8xhqL3nWJxFSK" +
        "h8bU0C6hqSzkZqazHKJe02R0qJHTBEgqRo/VfGReCz8W8VKlwBuJXQn3S8tK6Xvl" +
        "qKBlp/vmxA5Qzb5rA7QkLAzVXvBNtBlA6LoK47kVfCmjCzZgPMvVQpOcLpyvfptH" +
        "nHMxhLEvFE0XNqZGHBvwQ1P9fWdXhJCqJ3LSkT+djkrNLz4Yh4uJTLTFDv2n/aJz" +
        "dWuCZ2MJyLkImEjqNdBG3qGdSxjCq2BqB+DI3qAjkjqiShJH3zqVhcwVkHCJPT3" +
        "UZBqhAgMBAAECggEALtQO5N3vLqWYZFPQjvcJFP3MvNYWQ5RtPQVQJvF8L6Kqc3m" +
        "l0qU7XQd3Qz9L6Y8Uu/ZMM3hjEY5UxQZJqxDN7bHJ+VQBQ8C9rNJGPbPHQB3tLo" +
        "kUYO5lfPY8IJn5xXDXkP1eL5nxE3pBVN4lFPqD9QZdLxQQ1d9xYCqJfV5O0D8Py" +
        "h6H0pQHIKd+8Y5kCqN6wVP3L5Y8N3aRxP9hL0G5qQ8K6Z0h8P5Y7LKPV9qYf8Lv" +
        "D9QqN8Z0L5h8K0P9hLV8Q5Y0K6P8L9hV0QD8Y5K6h0L9P8V5Q0K6h8L9P5Y0K6h" +
        "0L9P8V5Q0K6h8L9P5Y0K6h0L9P8V5Q0K6h8L9P5Y0K6h0L9P8V5Q0K6h8L9P5Y0" +
        "QKBgQDmvC3L0hgUZHqXQ3z7oPBYVJPqR8WBdkUwYxPc3VL5qhQpYKJ0dY3Lw5pQ" +
        "hN0wYxPc3VL5qhQpYKJ0dY3Lw5pQhN0wYxPc3VL5qhQpYKJ0dY3Lw5pQhN0wYxP" +
        "c3VL5qhQpYKJ0dY3Lw5pQhN0wYxPc3VL5qhQpYKJ0dY3Lw5pQhN0wQKBgQDQmKJ" +
        "0dY3Lw5pQhN0wYxPc3VL5qhQpYKJ0dY3Lw5pQhN0wYxPc3VL5qhQpYKJ0dY3Lw5" +
        "pQhN0wYxPc3VL5qhQpYKJ0dY3Lw5pQhN0wYxPc3VL5qhQpYKJ0dY3Lw5pQhN0w" +
        "YxPc3VL5qhQpYKJ0dY3Lw5pQhN0wYxPc3VL5qhQpYKJ0dY3Lw5pQhN0wQKBg";
    private static final String PUBLIC_KEY_PEM = 
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1SU1LfVLPHCozMxH2Mo" +
        "4lgOEePzNm0tfn1iHD5teQPLzKds78gc/EOs32fMRc0GPMYai951icRUiofG1NAu" +
        "oakk5Gamsxyintlkdhh+b0BUqxkh14PDKYgqZjcR04RIKkaP1XxkXgs/FvFSpcAb" +
        "iV0J90vLSul75aigZaf75sQOUM2+awO0JCwM1V7wTbQZQOi6CuO5FXwpows2YDzL" +
        "1UKTnC6cr36bR5xzMYSxLxRNFzamRhwb8ENT/X1nV4SQqidy0pE/nY5KzS8+GIeL" +
        "iUy0xQ79p/2ic3VrgmdjCci5CJhI6jXQRt6hnUsYwqtgagfgyN6gI5I6okoSR986" +
        "lYXMFZBwiT091GQaoQIDAQAB";
    public String createAccessToken(TokenClaims claims) {
        try {
            Algorithm algorithm = getAlgorithm();
            return JWT.create()
                .withIssuer(jwtConfig.getIssuer())
                .withAudience(jwtConfig.getAudience())
                .withSubject(claims.getSub().toString())
                .withClaim("email", claims.getEmail())
                .withClaim("roles", claims.getRoles())
                .withClaim("permissions", claims.getPermissions())
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(jwtConfig.getAccessTokenExpiry())))
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
        } catch (Exception e) {
            log.error("Error creating access token", e);
            throw new TokenInvalidException("Failed to create token");
        }
    }
    public TokenClaims validateToken(String token) {
        try {
            Algorithm algorithm = getAlgorithm();
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(jwtConfig.getIssuer())
                .withAudience(jwtConfig.getAudience())
                .build();
            DecodedJWT jwt = verifier.verify(token);
            TokenClaims claims = new TokenClaims();
            claims.setSub(UUID.fromString(jwt.getSubject()));
            claims.setEmail(jwt.getClaim("email").asString());
            claims.setRoles(jwt.getClaim("roles").asList(String.class));
            claims.setPermissions(jwt.getClaim("permissions").asList(String.class));
            claims.setIss(jwt.getIssuer());
            claims            claims            claims            claimsset            claims            t());
            claims.setExp(jwt.getExpiresAt().toInstant());
            claims.setJti(UUID.fromString(jwt.getId()));
            if (claims.isExpired()) {
                throw new TokenExpiredException();
            }
            return claims;
        } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
            throw new TokenExpiredException();
        } catch (Exception e) {
            log.error("Error validating token", e);
            throw new TokenInvalidException("Invalid token");
        }
    }
    public UUID extractTokenId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return UUID.fromString(jwt.getId());
        } catch (Exception e) {
            throw new TokenInvalidException("Cannot extract token ID");
        }
    }
    private Algorithm getAlgorithm() {
        try {
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY_PEM)));
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY_PEM)));
            return Algorithm.RSA256(publicKey, privateKey);
        } catch (Exception e) {
            throw ne            throw ne            throw ne  s", e            throw ne EOF
# PasswordManager
cat > "$BASE/security/PasswordManager.java" << 'EOF'
package com.authservice.security;
import com.authservice.dto.FieldError;
import lombok.RequiredArgsConstructor;
iiiiiiiiimbok.eiiiiiiiiimbok.eiiiiiiiiimbok.eiiiiiiiiamework.security.crypto.bcrypt.BCryptPasswordEncoder;
imporimporimporimporimporimptereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
/**
 * Password Manager for bcrypt hashing and validation
 * Requirement: AUTH-FR-001, AUTH-FR-002
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordManager {
    private final PasswordConfig passwordConfig;
    private final BCryptPasswordEncoder bcryptEncoder;
    public PasswordManager(PasswordConfig config) {
        this.passwordConfig = config;
        this.bcryptEncoder = new BCryptPassword        this.bcryptEncoder = new BCryptPass    public String hashPassword(String plainPassword) {
        return bcryptEncoder.encode(plainPassword);
    }
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return bcryptEncoder.matches(plainPassword, hashedPassword);
    }
    public ValidationResult validatePasswordPolicy(String password) {
        List<FieldError> errors = new ArrayList<>();
        if (password == null || password.length() < passwordConfig.getPolicy().getMinLength()) {
            errors.add(FieldError.builder(            errors.add(Fielassword")
                .message("Password must be at least " + passwordConfig.getPolicy().getMinLength() + " characters")
                .build());
        }
        if (passwordConfig.getPolicy().isRequireUppercase() && !Pattern.compile("[A-Z]").matcher(password).find()) {
            errors.add(FieldError.builder()
                .field("password")
                .message("Password must contain at least one uppercase letter")
                .build());
        }
        if (passwordConfig.getPolicy().isRequireLowercase() && !Pattern.compile("[a-z]").matcher(password).find()) {
            errors.add(FieldError.builder()
                .field("password")
                .message("Password must contain at least one lowercase letter")
                .build());
        }
        if (passwordConfig.getPolicy().isRequireDigit() && !Pattern.compile("[0-9]").matcher(password).find()) {
            errors.add(FieldError.builder()
                .field("password")
                .message("Password must contain at least one digit")
                .build());
        }
        if (passwordConfig.getPolicy().isRequireSpecialChar() && 
            !Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(password).find()) {
            errors.add(FieldError.builder()
                .field("password")
                .message("Password must contain at least one special character")
                .build());
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }
    public static class ValidationResult {
        private final boolean valid;
        private        private        private          public ValidationResult(boolean valid, List<FieldError> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        public        public        public        public        public        public        public        public                 return errors;
        }
    }
}
EOF
# PasswordConfig
cat > "$BASE/config/PasswordConfig.java" << 'EOF'
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
EOF
echo "✅ JWTManager, PasswordManager, PasswordConfecho "�ted"
