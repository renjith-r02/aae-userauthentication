package com.authservice.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {
    private UUID userId;
    private String email;
    private List<String> roles;
}
