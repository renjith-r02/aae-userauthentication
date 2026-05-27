package com.authservice.controller;

import com.authservice.dto.ComponentHealth;
import com.authservice.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Health Check Controller
 * Provides health and readiness endpoints for Kubernetes
 */
@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health", description = "Application health and readiness endpoints")
public class HealthController {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check overall application health")
    public ResponseEntity<HealthResponse> health() {
        List<ComponentHealth> components = new ArrayList<>();
        boolean healthy = true;
        
        // Check database
        ComponentHealth dbHealth = checkDatabase();
        components.add(dbHealth);
        if (!"UP".equals(dbHealth.getStatus())) {
            healthy = false;
        }
        
        // Check Redis
        ComponentHealth redisHealth = checkRedis();
        components.add(redisHealth);
        if (!"UP".equals(redisHealth.getStatus())) {
            healthy = false;
        }
        
        HealthResponse response = HealthResponse.builder()
                .status(healthy ? "UP" : "DOWN")
                .components(components)
                .build();
        
        return healthy ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }
    
    @GetMapping("/health/liveness")
    @Operation(summary = "Liveness probe", description = "Check if application is alive")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("UP");
    }
    
    @GetMapping("/health/readiness")
    @Operation(summary = "Readiness probe", description = "Check if application is ready to serve requests")
    public ResponseEntity<HealthResponse> readiness() {
        return health();
    }
    
    private ComponentHealth checkDatabase() {
        try {
            try (Connection connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(2);
                return ComponentHealth.builder()
                        .name("database")
                        .status(valid ? "UP" : "DOWN")
                        .message(valid ? "PostgreSQL connection successful" : "Connection validation failed")
                        .build();
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return ComponentHealth.builder()
                    .name("database")
                    .status("DOWN")
                    .message("Database connection failed: " + e.getMessage())
                    .build();
        }
    }
    
    private ComponentHealth checkRedis() {
        try {
            redisTemplate.opsForValue().set("health:check", "OK");
            String value = (String) redisTemplate.opsForValue().get("health:check");
            boolean valid = "OK".equals(value);
            
            return ComponentHealth.builder()
                    .name("redis")
                    .status(valid ? "UP" : "DOWN")
                    .message(valid ? "Redis connection successful" : "Redis validation failed")
                    .build();
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return ComponentHealth.builder()
                    .name("redis")
                    .status("DOWN")
                    .message("Redis connection failed: " + e.getMessage())
                    .build();
        }
    }
}

