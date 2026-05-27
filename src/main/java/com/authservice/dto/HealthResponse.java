package com.authservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {
    private String status;
    private Map<String, ComponentHealth> components;
    private LocalDateTime timestamp;
}
