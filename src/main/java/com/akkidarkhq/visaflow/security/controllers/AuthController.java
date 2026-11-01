package com.akkidarkhq.visaflow.security.controllers;

import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.response.UserResponse;
import com.akkidarkhq.visaflow.security.payload.request.LoginRequest;
import com.akkidarkhq.visaflow.security.payload.request.SignupRequest;
import com.akkidarkhq.visaflow.security.payload.response.JwtResponse;
import com.akkidarkhq.visaflow.security.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user registration, authentication, session profile retrieval, and JWT token issuance.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user signup and signin to acquire JWT bearer tokens, and current user profile retrieval")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a signed JWT token with user details and roles wrapped in ApiResponse.
     *
     * @param loginRequest Username and password
     * @return ApiResponse containing JwtResponse (bearer token, user ID, email, and roles)
     */
    @PostMapping("/signin")
    @Operation(
            summary = "User login / signin",
            description = "Authenticates user credentials and issues a JWT token to be used in the Authorization header (Bearer <token>)."
    )
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", jwtResponse));
    }

    /**
     * Registers a new user account with default CLIENT role or specified role wrapped in ApiResponse.
     *
     * @param signUpRequest User profile and registration details
     * @return ApiResponse with success confirmation and registered user details
     */
    @PostMapping("/signup")
    @Operation(
            summary = "User registration / signup",
            description = "Registers a new user account with username, email, full name, phone number, password, and optional role (CLIENT, ADMIN)."
    )
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        UserResponse createdUser = authService.registerUser(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully!", createdUser));
    }

    /**
     * Retrieves the profile of the currently signed-in user from the JWT session or optional username/email query.
     *
     * @param authentication Injected Spring Security authentication
     * @param username       Optional username parameter
     * @param email          Optional email parameter
     * @return UserResponse wrapped in ApiResponse
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get currently signed-in user profile",
            description = "Retrieves user details for the authenticated session, or using optional username/email lookup."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            Authentication authentication,
            @Parameter(description = "Optional username to lookup directly")
            @RequestParam(required = false) String username,
            @Parameter(description = "Optional email to lookup directly")
            @RequestParam(required = false) String email) {

        UserResponse userResponse = authService.getCurrentUserProfile(authentication, username, email);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", userResponse));
    }
}
