package com.akkidarkhq.visaflow.ai.service;

import com.akkidarkhq.visaflow.ai.client.AiClient;
import com.akkidarkhq.visaflow.ai.client.AiClientFactory;
import com.akkidarkhq.visaflow.ai.dto.ChatRequest;
import com.akkidarkhq.visaflow.ai.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * High-level orchestration service for the VisaFlow AI Assistant.
 * Combines RAG database grounding with pluggable AI model clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final VisaKnowledgeService visaKnowledgeService;
    private final AiClientFactory aiClientFactory;

    /**
     * Processes a user chat request and generates an intelligent, grounded response.
     */
    public ChatResponse processChat(ChatRequest request) {
        log.info("Processing chat query: '{}' [Country hint: {}, VisaType hint: {}, Ref hint: {}]",
                request.getMessage(), request.getCountryCode(), request.getVisaTypeId(), request.getReferenceNumber());

        // 1. Gather database domain facts (RAG)
        VisaKnowledgeService.GroundedContext groundedContext = visaKnowledgeService.buildGroundedContext(request);
        String systemPrompt = visaKnowledgeService.buildSystemPrompt(groundedContext);

        // 2. Select AI Provider Client
        AiClient primaryClient = aiClientFactory.getActiveClient();
        String replyText;
        String providerId = primaryClient.getProviderId();

        try {
            replyText = primaryClient.chat(systemPrompt, request.getHistory(), request.getMessage());
        } catch (Exception e) {
            log.warn("Primary AI provider '{}' failed: {}. Falling back to local RAG provider.",
                    primaryClient.getProviderId(), e.getMessage());

            AiClient fallback = aiClientFactory.getFallbackClient();
            providerId = fallback.getProviderId() + " (fallback)";
            replyText = fallback.chat(systemPrompt, request.getHistory(), request.getMessage());
        }

        return ChatResponse.builder()
                .reply(replyText)
                .provider(providerId)
                .contextGrounding(groundedContext.isHasData())
                .suggestedFollowUps(groundedContext.getFollowUps())
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
