package com.akkidarkhq.visaflow.security.services;

import com.akkidarkhq.visaflow.dto.response.UserResponse;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.security.jwt.JwtUtils;
import com.akkidarkhq.visaflow.security.model.User;
import com.akkidarkhq.visaflow.security.model.UserRole;
import com.akkidarkhq.visaflow.security.payload.request.LoginRequest;
import com.akkidarkhq.visaflow.security.payload.request.SignupRequest;
import com.akkidarkhq.visaflow.security.payload.response.JwtResponse;
import com.akkidarkhq.visaflow.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(sampleUserId)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .passwordHash("encodedPassword")
                .role(UserRole.CLIENT)
                .isActive(true)
                .build();
    }

    @Test
    void authenticateUser_Success() {
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        UserDetailsImpl userDetails = new UserDetailsImpl(
                sampleUserId,
                "testuser",
                "test@example.com",
                "encodedPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("mock.jwt.token");

        JwtResponse response = authService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(sampleUserId, response.getId());
        assertEquals(Collections.singletonList("ROLE_CLIENT"), response.getRoles());
    }

    @Test
    void registerUser_Success() {
        SignupRequest signupRequest = SignupRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .fullName("New User")
                .phoneNumber("+9876543210")
                .password("password123")
                .role("CLIENT")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.registerUser(signupRequest);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
        assertEquals(UserRole.CLIENT, response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_ThrowsException_WhenUsernameTaken() {
        SignupRequest signupRequest = SignupRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerUser(signupRequest));

        assertEquals("Username is already taken!", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ThrowsException_WhenEmailTaken() {
        SignupRequest signupRequest = SignupRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerUser(signupRequest));

        assertEquals("Email is already in use!", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ThrowsException_WhenRoleInvalid() {
        SignupRequest signupRequest = SignupRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .role("SUPER_USER")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerUser(signupRequest));

        assertTrue(ex.getMessage().contains("Invalid role specified"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getCurrentUserProfile_WithUsernameParam_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getCurrentUserProfile(null, "testuser", null);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void getCurrentUserProfile_WithEmailParam_Success() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getCurrentUserProfile(null, null, "test@example.com");

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void getCurrentUserProfile_ThrowsInsufficientAuthentication_WhenNoContextOrParams() {
        assertThrows(InsufficientAuthenticationException.class,
                () -> authService.getCurrentUserProfile(null, null, null));
    }

    @Test
    void getCurrentUserProfile_ThrowsResourceNotFound_WhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getCurrentUserProfile(null, "nonexistent", null));
    }
}
