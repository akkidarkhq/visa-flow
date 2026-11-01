package com.akkidarkhq.visaflow.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single message in a chat conversation history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Single chat turn message")
public class ChatMessageDto {

    @NotBlank
    @Schema(description = "Message sender role: 'user', 'assistant', or 'system'", example = "user")
    private String role;

    @NotBlank
    @Schema(description = "Text content of the message", example = "What documents are required for France?")
    private String content;
}
