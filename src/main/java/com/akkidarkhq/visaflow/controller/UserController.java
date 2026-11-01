package com.akkidarkhq.visaflow.controller;

import com.akkidarkhq.visaflow.dto.ApiError;
import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.response.UserResponse;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.security.model.User;
import com.akkidarkhq.visaflow.security.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing and retrieving user profile details.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for retrieving user profiles and currently signed-in user details")
public class UserController {

    private final UserRepository userRepository;

    /**
     * Retrieves the profile of the currently authenticated user (or via optional username/email query).
     *
     * @param authentication Injected Spring Security authentication from JWT bearer token
     * @param username       Optional username parameter
     * @param email          Optional email parameter
     * @return UserResponse wrapped in standard ApiResponse
     */
    @GetMapping("/me")
    @Operation(summary = "Get currently signed-in user profile",
            description = "Retrieves user details for the authenticated session, or using optional username/email lookup.")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            Authentication authentication,
            @Parameter(description = "Optional username to lookup directly")
            @RequestParam(required = false) String username,
            @Parameter(description = "Optional email to lookup directly")
            @RequestParam(required = false) String email) {

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
            ApiError apiError = ApiError.of(
                    "UNAUTHORIZED",
                    HttpStatus.UNAUTHORIZED.value(),
                    "Full authentication is required to access this resource"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", apiError));
        }

        final String lookupKey = identifier;
        User user = userRepository.findByUsername(lookupKey)
                .or(() -> userRepository.findByEmail(lookupKey))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username or email: " + lookupKey));

        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", UserResponse.fromEntity(user)));
    }
}
