package com.akkidarkhq.visaflow.security.controllers;

import com.akkidarkhq.visaflow.dto.response.UserResponse;
import com.akkidarkhq.visaflow.exception.GlobalExceptionHandler;
import com.akkidarkhq.visaflow.security.model.UserRole;
import com.akkidarkhq.visaflow.security.payload.request.LoginRequest;
import com.akkidarkhq.visaflow.security.payload.request.SignupRequest;
import com.akkidarkhq.visaflow.security.payload.response.JwtResponse;
import com.akkidarkhq.visaflow.security.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void authenticateUser_ValidRequest_ReturnsOkWithToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        JwtResponse jwtResponse = new JwtResponse(
                "mock.token.jwt",
                UUID.randomUUID(),
                "testuser",
                "test@example.com",
                Collections.singletonList("ROLE_CLIENT")
        );

        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(jwtResponse);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Authentication successful"))
                .andExpect(jsonPath("$.data.token").value("mock.token.jwt"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void authenticateUser_BlankFields_ReturnsBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void registerUser_ValidRequest_ReturnsCreated() throws Exception {
        SignupRequest signupRequest = SignupRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .fullName("New User")
                .phoneNumber("+1234567890")
                .password("securePass123")
                .role("CLIENT")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("newuser")
                .email("new@example.com")
                .fullName("New User")
                .phoneNumber("+1234567890")
                .role(UserRole.CLIENT)
                .isActive(true)
                .build();

        when(authService.registerUser(any(SignupRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully!"))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void registerUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        SignupRequest invalidRequest = SignupRequest.builder()
                .username("newuser")
                .email("not-an-email")
                .fullName("New User")
                .phoneNumber("+1234567890")
                .password("123") // too short
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getCurrentUser_WithUsernameParam_ReturnsProfile() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.CLIENT)
                .isActive(true)
                .build();

        when(authService.getCurrentUserProfile(any(), any(), any())).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me")
                        .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }
}
