package com.akkidarkhq.visaflow.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload sent by user or frontend when asking the AI chatbot a question.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chat request containing user query and optional context hints")
public class ChatRequest {

    @NotBlank(message = "Message must not be blank")
    @Schema(description = "The user's query or prompt", example = "What are the required documents for a Tourist Visa to France?")
    private String message;

    @Builder.Default
    @Schema(description = "Prior conversation turns for multi-turn dialogue context")
    private List<ChatMessageDto> history = new ArrayList<>();

    @Schema(description = "Optional ISO-2/ISO-3 country code hint to scope context (e.g., 'FR', 'FRA')", example = "FRA")
    private String countryCode;

    @Schema(description = "Optional Visa Type ID hint", example = "1")
    private Long visaTypeId;

    @Schema(description = "Optional Visa Application Reference (VIS-...) or Group Reference (GRP-...) to check status", example = "GRP-2026-X8K9M2")
    private String referenceNumber;
}
