package com.akkidarkhq.visaflow.ai.client;

import com.akkidarkhq.visaflow.ai.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * OpenAI Chat Completions client implementation (supports gpt-4o, gpt-4o-mini, etc.).
 */
@Slf4j
@Component
public class OpenAiClient implements AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    @Autowired
    public OpenAiClient(
            @Autowired(required = false) ObjectMapper objectMapper,
            @Value("${visaflow.ai.openai.api-key:}") String apiKey,
            @Value("${visaflow.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${visaflow.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model : "gpt-4o-mini";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com/v1";
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    @Override
    public String getProviderId() {
        return "openai:" + model;
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank();
    }

    @Override
    public String chat(String systemInstruction, List<ChatMessageDto> history, String userMessage) {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemInstruction));
            }

            if (history != null) {
                for (ChatMessageDto msg : history) {
                    if (msg.getContent() != null && !msg.getContent().isBlank()) {
                        String role = normalizeRole(msg.getRole());
                        messages.add(Map.of("role", role, "content", msg.getContent()));
                    }
                }
            }

            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 0.4);

            String responseJson = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode messageNode = choices.get(0).path("message");
                return messageNode.path("content").asText();
            }

            throw new RuntimeException("Unexpected OpenAI response format: " + responseJson);
        } catch (Exception e) {
            log.error("Failed to call OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API invocation failed: " + e.getMessage(), e);
        }
    }

    private String normalizeRole(String role) {
        if (role == null) return "user";
        String r = role.trim().toLowerCase();
        return switch (r) {
            case "assistant", "bot", "ai" -> "assistant";
            case "system" -> "system";
            default -> "user";
        };
    }
}
