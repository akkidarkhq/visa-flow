package com.akkidarkhq.visaflow.ai.controller;

import com.akkidarkhq.visaflow.ai.client.AiClientFactory;
import com.akkidarkhq.visaflow.ai.dto.ChatRequest;
import com.akkidarkhq.visaflow.ai.dto.ChatResponse;
import com.akkidarkhq.visaflow.ai.service.AiChatService;
import com.akkidarkhq.visaflow.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for VisaFlow AI Chatbot.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chatbot", description = "Endpoints for AI-powered visa guidance, checklist inquiries, and application tracking")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiClientFactory aiClientFactory;

    @PostMapping
    @Operation(summary = "Send a message to the AI Chatbot",
            description = "Submits a user query with optional conversation history, destination country, or application reference number. Returns grounded response and follow-up prompts.")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = aiChatService.processChat(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/providers")
    @Operation(summary = "Get AI provider statuses",
            description = "Returns current active AI provider and availability status of OpenAI, Gemini, and Local providers.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviders() {
        Map<String, Object> data = Map.of(
                "activeProvider", aiClientFactory.getActiveClient().getProviderId(),
                "providers", aiClientFactory.getProvidersStatus()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
