package com.akkidarkhq.visaflow.security.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for user authentication")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username or email of the registered user", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "SecurePassword123!")
    private String password;
}
