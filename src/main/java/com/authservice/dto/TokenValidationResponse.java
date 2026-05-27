package com.authservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private UUID userId;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private Instant expiresAt;
}
