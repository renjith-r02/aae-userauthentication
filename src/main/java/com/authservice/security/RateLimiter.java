package com.authservice.security;

import com.authservice.config.RateLimitConfig;
import com.authservice.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiter using Redis
 * Requirement: AUTH-FR-002 (Rate limiting to prevent brute force)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimiter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitConfig rateLimitConfig;
    
    private static final String RATE_LIMIT_PREFIX = "auth:ratelimit:";
    
    public void checkRateLimit(String key, RateLimitType type) {
        int maxAttempts;
        Duration window;
        
        switch (type) {
            case LOGIN:
                maxAttempts = rateLimitConfig.getLogin().getMaxAttempts();
                window = rateLimitConfig.getLogin().getWindowDuration();
                break;
            case REGISTRATION:
                maxAttempts = rateLimitConfig.getRegistration().getMaxAttempts();
                window = rateLimitConfig.getRegistration().getWindowDuration();
                break;
            case REFRESH:
                maxAttempts = rateLimitConfig.getRefresh().getMaxAttempts();
                window = rateLimitConfig.getRefresh().getWindowDuration();
                break;
            case API:
                maxAttempts = rateLimitConfig.getApi().getMaxAttempts();
                window = rateLimitConfig.getApi().getWindowDuration();
                break;
            default:
                maxAttempts = 100;
                window = Duration.ofMinutes(1);
        }
        
        String redisKey = RATE_LIMIT_PREFIX + type.name().toLowerCase() + ":" + key;
        
        // Get current count
        Integer currentCount = (Integer) redisTemplate.opsForValue().get(redisKey);
        
        if (currentCount == null) {
            // First attempt
            redisTemplate.opsForValue().set(redisKey, 1, window.getSeconds(), TimeUnit.SECONDS);
            log.debug("Rate limit initialized for key: {}", redisKey);
        } else if (currentCount >= maxAttempts) {
            // Rate limit exceeded
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            int retryAfter = ttl != null ? ttl.intValue() : (int) window.getSeconds();
            
            log.warn("Rate limit exceeded for key: {}, current: {}, max: {}", redisKey, currentCount, maxAttempts);
            throw new RateLimitExceededException(retryAfter);
        } else {
            // Increment count
            redisTemplate.opsForValue().increment(redisKey);
            log.debug("Rate limit incremented for key: {}, count: {}/{}", redisKey, currentCount + 1, maxAttempts);
        }
    }
    
    public void resetRateLimit(String key, RateLimitType type) {
        String redisKey = RATE_LIMIT_PREFIX + type.name().toLowerCase() + ":" + key;
        redisTemplate.delete(redisKey);
        log.debug("Rate limit reset for key: {}", redisKey);
    }
    
    public enum RateLimitType {
        LOGIN,
        REGISTRATION,
        REFRESH,
        API
    }
}

