package com.akkidarkhq.visaflow.security.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for user registration")
public class SignupRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Unique username for the account", example = "john_doe")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Schema(description = "Unique email address", example = "john.doe@example.com")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    @Schema(description = "Full legal name of the user", example = "John Doe")
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    @Schema(description = "Account password (min 6 characters)", example = "SecurePassword123!")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Schema(description = "Contact phone number with country code", example = "+919876543210")
    private String phoneNumber;

    @Schema(description = "User role in the system (e.g. CLIENT or ADMIN). Defaults to CLIENT.", example = "CLIENT")
    private String role;
}
