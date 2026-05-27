package com.authservice.domain.enums;

/**
 * Refresh token status enumeration
 * Requirement: AUTH-FR-004 (Refresh Token Flow)
 */
public enum RefreshTokenStatus {
    /**
     * Token is active and can be used
     */
    ACTIVE,
    
    /**
     * Token has been rotated and replaced with a new one
     */
    ROTATED,
    
    /**
     * Token has been explicitly revoked
     */
    REVOKED,
    
    /**
     * Token has expired based on its expiration time
     */
    EXPIRED
}

