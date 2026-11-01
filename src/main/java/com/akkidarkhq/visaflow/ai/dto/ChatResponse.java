package com.akkidarkhq.visaflow.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Chatbot response payload returned to frontend/client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chatbot response containing the assistant message, provider info, and suggested follow-ups")
public class ChatResponse {

    @Schema(description = "Assistant reply message formatted with markdown", example = "For France Tourist Visa, you need: 1. Passport with 6-month validity...")
    private String reply;

    @Schema(description = "The AI provider that served the response (e.g., 'openai:gpt-4o-mini', 'gemini:gemini-1.5-flash', 'visaflow:local-rag')", example = "visaflow:local-rag")
    private String provider;

    @Schema(description = "Whether real VisaFlow database context was retrieved and injected into the response", example = "true")
    private boolean contextGrounding;

    @Builder.Default
    @Schema(description = "Suggested follow-up questions relevant to the current conversation")
    private List<String> suggestedFollowUps = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Timestamp when the response was generated")
    private OffsetDateTime timestamp = OffsetDateTime.now();
}
