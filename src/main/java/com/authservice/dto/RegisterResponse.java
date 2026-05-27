package com.authservice.dto;

import com.authservice.domain.enums.UserStatus;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private UUID userId;
    private String email;
    private UserStatus status;
    private String message;
}
