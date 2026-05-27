package com.authservice.integration;

import com.authservice.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test for Token Refresh Flow
 * Tests token refresh and token rotation
 * Requirement: AUTH-FR-004 (Token Refresh)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Token Refresh Integration Tests")
class TokenRefreshIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:14-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String accessToken;
    private String refreshToken;
    
    @BeforeEach
    void setUp() throws Exception {
        // Register and login to get tokens
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe." + System.currentTimeMillis() + "@example.com")
                .password("SecurePass@123")
                .build();
        
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
        
        LoginRequest loginRequest = LoginRequest.builder()
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build();
        
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String loginResponseBody = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(loginResponseBody, LoginResponse.class);
        
        accessToken = loginResponse.getAccessToken();
        refreshToken = loginResponse.getRefreshToken();
    }
    
    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void testTokenRefresh_Success() throws Exception {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();
        
        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists())
                .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        TokenResponse response = objectMapper.readValue(responseBody, TokenResponse.class);
        
        // Verify new tokens are different from old ones
        assertThat(response.getAccessToken()).isNotEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isNotEqualTo(refreshToken);
    }
    
    @Test
    @DisplayName("Should reject refresh with invalid refresh token")
    void testTokenRefresh_InvalidToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Should detect and prevent refresh token reuse (replay attack)")
    void testTokenRefresh_ReplayAttack() throws Exception {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();
        
        // Act 1 - First refresh (should succeed)
        MvcResult firstRefreshResult = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        
        String firstRefreshBody = firstRefreshResult.getResponse().getContentAsString();
        TokenResponse firstRefreshResponse = objectMapper.readValue(firstRefreshBody, TokenResponse.class);
        
        assertThat(firstRefreshResponse.getRefreshToken()).isNotEqualTo(refreshToken);
        
        // Act 2 - Try to reuse old refresh token (should fail - replay attack detected)
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @DisplayName("Should handle multiple token refresh operations (token rotation)")
    void testTokenRefresh_MultipleRefreshes() throws Exception {
        String currentToken = refreshToken;
        
        // Perform 3 consecutive refreshes
        for (int i = 0; i < 3; i++) {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(currentToken)
                    .build();
            
            MvcResult result = mockMvc.perform(post("/api/v1/auth/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andReturn();
            
            String responseBody = result.getResponse().getContentAsString();
            TokenResponse response = objectMapper.readValue(responseBody, TokenResponse.class);
            
            // Verify new token is different
            assertThat(response.getRefreshToken()).isNotEqualTo(currentToken);
            
            // Use new token for next iteration
            currentToken = response.getRefreshToken();
        }
    }
}

