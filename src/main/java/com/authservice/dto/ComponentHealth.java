package com.authservice.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentHealth {
    private String status;
    private Map<String, Object> details;
}
