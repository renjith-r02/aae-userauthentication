package com.authservice.domain.enums;

/**
 * Session status enumeration
 * Requirement: AUTH-FR-005 (Session Invalidation)
 */
public enum SessionStatus {
    /**
     * Session is active
     */
    ACTIVE,
    
    /**
     * Session has been revoked (logout or security event)
     */
    REVOKED,
    
    /**
     * Session has expired based on inactivity
     */
    EXPIRED
}

