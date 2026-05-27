package com.authservice.domain.enums;

/**
 * User account status enumeration
 * Requirement: AUTH-FR-001 (User Registration), AUTH-FR-002 (User Authentication)
 */
public enum UserStatus {
    /**
     * User is active and can authenticate
     */
    ACTIVE,
    
    /**
     * User account is pending verification
     */
    PENDING,
    
    /**
     * User account is temporarily locked due to security reasons
     */
    LOCKED,
    
    /**
     * User account is disabled by administrator
     */
    DISABLED
}

