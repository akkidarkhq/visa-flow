package com.akkidarkhq.visaflow.ai.client;

import com.akkidarkhq.visaflow.ai.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Google Gemini API client implementation (supports gemini-3.5-flash, gemini-2.5, etc.).
 */
@Slf4j
@Component
public class GeminiClient implements AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    @Autowired
    public GeminiClient(
            @Autowired(required = false) ObjectMapper objectMapper,
            @Value("${visaflow.ai.gemini.api-key:}") String apiKey,
            @Value("${visaflow.ai.gemini.model:gemini-3.5-flash}") String model,
            @Value("${visaflow.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model : "gemini-3.5-flash";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://generativelanguage.googleapis.com";
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    @Override
    public String getProviderId() {
        return "gemini:" + model;
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank();
    }

    @Override
    public String chat(String systemInstruction, List<ChatMessageDto> history, String userMessage) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        try {
            Map<String, Object> body = new HashMap<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                body.put("system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ));
            }

            List<Map<String, Object>> contents = new ArrayList<>();
            if (history != null) {
                for (ChatMessageDto msg : history) {
                    if (msg.getContent() != null && !msg.getContent().isBlank()) {
                        String role = normalizeGeminiRole(msg.getRole());
                        contents.add(Map.of(
                                "role", role,
                                "parts", List.of(Map.of("text", msg.getContent()))
                        ));
                    }
                }
            }

            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
            ));
            body.put("contents", contents);

            String uri = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            String responseJson = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }

            throw new RuntimeException("Unexpected Gemini response format: " + responseJson);
        } catch (Exception e) {
            log.error("Failed to call Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini API invocation failed: " + e.getMessage(), e);
        }
    }

    private String normalizeGeminiRole(String role) {
        if (role == null) return "user";
        String r = role.trim().toLowerCase();
        return switch (r) {
            case "assistant", "bot", "ai", "model" -> "model";
            default -> "user";
        };
    }
}
