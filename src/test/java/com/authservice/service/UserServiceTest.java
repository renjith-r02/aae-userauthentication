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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for UserService
 * Requirement: AUTH-FR-001, AUTH-FR-006
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private UserRoleRepository userRoleRepository;
    
    @Mock
    private PasswordManager passwordManager;
    
    @Mock
    private RBACService rbacService;
    
    @Mock
    private AuditLogger auditLogger;
    
    @InjectMocks
    private UserService userService;
    
    private RegisterRequest validRequest;
    private User testUser;
    private Role userRole;
    
    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("SecurePass@123")
                .build();
        
        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();
        
        userRole = Role.builder()
                .id(UUID.randomUUID())
                .name("USER")
                .description("Default user role")
                .build();
    }
    
    @Test
    @DisplayName("Should create user successfully with valid data")
    void testCreateUser_Success() {
        // Arrange
        when(passwordManager.validatePasswordPolicy(anyString()))
                .thenReturn(new PasswordManager.ValidationResult(true, Collections.emptyList()));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole());
        when(passwordManager.hashPassword(anyString())).thenReturn("$2a$12$hashedPassword");
        
        // Act
        RegisterResponse response = userService.createUser(validRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(validRequest.getEmail());
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        
        verify(userRepository).save(any(User.class));
        verify(userRoleRepository).save(any(UserRole.class));
        verify(auditLogger).logUserRegistration(any(UUID.class), anyString());
    }
    
    @Test
    @DisplayName("Should throw ValidationException when password policy fails")
    void testCreateUser_InvalidPassword() {
        // Arrange
        com.authservice.dto.FieldError fieldError = com.authservice.dto.FieldError.builder()
                .field("password")
                .message("Password too weak")
                .build();
        
        when(passwordManager.validatePasswordPolicy(anyString()))
                .thenReturn(new PasswordManager.ValidationResult(false, Arrays.asList(fieldError)));
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(ValidationException.class);
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw ConflictException when email already exists")
    void testCreateUser_EmailExists() {
        // Arrange
        when(passwordManager.validatePasswordPolicy(anyString()))
                .thenReturn(new PasswordManager.ValidationResult(true, Collections.emptyList()));
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already registered");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should retrieve user by ID successfully")
    void testGetUserById_Success() {
        // Arrange
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(rbacService.getUserRoles(userId)).thenReturn(Arrays.asList("USER"));
        when(rbacService.getUserPermissions(userId)).thenReturn(Arrays.asList("PROFILE_READ"));
        
        // Act
        UserResponse response = userService.getUserById(userId);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(response.getRoles()).contains("USER");
        assertThat(response.getPermissions()).contains("PROFILE_READ");
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void testGetUserById_NotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException when user is not active")
    void testGetUserById_NotActive() {
        // Arrange
        testUser.setStatus(UserStatus.DISABLED);
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    @DisplayName("Should find user by email successfully")
    void testFindByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        
        // Act
        User foundUser = userService.findByEmail(testUser.getEmail());
        
        // Assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo(testUser.getEmail());
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException when email not found")
    void testFindByEmail_NotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.findByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

