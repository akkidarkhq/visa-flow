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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service orchestrating authentication, user registration, and profile resolution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * Authenticates credentials, sets security context, and generates a signed JWT token.
     *
     * @param loginRequest Username and password
     * @return JwtResponse containing bearer token, user ID, email, and assigned roles
     */
    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user with username/identifier: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );
    }

    /**
     * Registers a new user account with validated unique username and email.
     *
     * @param signUpRequest User registration parameters
     * @return Created User converted to UserResponse
     */
    @Transactional
    public UserResponse registerUser(SignupRequest signUpRequest) {
        log.info("Processing registration for username: {} and email: {}", signUpRequest.getUsername(), signUpRequest.getEmail());

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already in use!");
        }

        UserRole userRole = UserRole.CLIENT;
        if (signUpRequest.getRole() != null && !signUpRequest.getRole().trim().isEmpty()) {
            try {
                userRole = UserRole.valueOf(signUpRequest.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role specified. Valid roles are CLIENT or ADMIN.");
            }
        }

        User user = User.builder()
                .username(signUpRequest.getUsername().trim())
                .email(signUpRequest.getEmail().trim().toLowerCase())
                .fullName(signUpRequest.getFullName().trim())
                .phoneNumber(signUpRequest.getPhoneNumber().trim())
                .passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
                .role(userRole)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user id: {}", savedUser.getId());
        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Resolves the profile of the current authenticated user session, or looks up by username/email query.
     *
     * @param authentication Injected Spring Security authentication
     * @param username       Optional username parameter
     * @param email          Optional email parameter
     * @return UserResponse dto
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(Authentication authentication, String username, String email) {
        String identifier = null;

        if (username != null && !username.trim().isEmpty()) {
            identifier = username.trim();
        } else if (email != null && !email.trim().isEmpty()) {
            identifier = email.trim();
        } else if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            identifier = authentication.getName();
        }

        if (identifier == null) {
            throw new InsufficientAuthenticationException("Full authentication is required to access this resource");
        }

        final String lookupKey = identifier;
        User user = userRepository.findByUsername(lookupKey)
                .or(() -> userRepository.findByEmail(lookupKey))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username or email: " + lookupKey));

        return UserResponse.fromEntity(user);
    }
}
