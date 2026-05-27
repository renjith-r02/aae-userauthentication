package com.authservice.service;

import com.authservice.domain.entity.Role;
import com.authservice.domain.entity.User;
import com.authservice.domain.entity.UserRole;
import com.authservice.domain.enums.UserStatus;
import com.authservice.dto.RegisterRequest;
import com.authservice.dto.RegisterResponse;
import com.authservice.dto.UserResponse;
import com.authservice.exception.ConflictException;
import com.authservice.exception.ResourceNotFoundException;
import com.authservice.exception.ValidationException;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.repository.UserRoleRepository;
import com.authservice.security.PasswordManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Service
 * Requirement: AUTH-FR-001 (User Registration), AUTH-FR-006 (User Details)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordManager passwordManager;
    private final RBACService rbacService;
    private final AuditLogger auditLogger;
    
    @Transactional
    public RegisterResponse createUser(RegisterRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        // Validate password policy
        PasswordManager.ValidationResult validation = passwordManager.validatePasswordPolicy(request.getPassword());
        if (!validation.isValid()) {
            throw new ValidationException(validation.getErrors());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }
        
        // Create user
        User user = User.builder()
                .id(UUID.randomUUID())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordManager.hashPassword(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();
        
        user = userRepository.save(user);
        
        // Assign default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default USER role not found"));
        
        UserRole assignment = UserRole.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .roleId(userRole.getId())
                .build();
        userRoleRepository.save(assignment);
        
        // Audit log
        auditLogger.logUserRegistration(user.getId(), user.getEmail());
        
        log.info("User created successfully: {}", user.getId());
        
        return RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .status(user.getStatus())
                .message("User registered successfully")
                .build();
    }
    
    public UserResponse getUserById(UUID userId) {
        log.info("Fetching user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        if (!user.isActive()) {
            throw new ResourceNotFoundException("User", userId);
        }
        
        List<String> roles = rbacService.getUserRoles(userId);
        List<String> permissions = rbacService.getUserPermissions(userId);
        
        return UserResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .roles(roles)
                .permissions(permissions)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", null));
    }
}

