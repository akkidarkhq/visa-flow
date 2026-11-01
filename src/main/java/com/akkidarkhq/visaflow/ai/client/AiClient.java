package com.akkidarkhq.visaflow.ai.client;

import com.akkidarkhq.visaflow.ai.dto.ChatMessageDto;

import java.util.List;

/**
 * Common pluggable interface for all AI model providers (OpenAI, Gemini, Local, etc.).
 */
public interface AiClient {

    /**
     * Unique identifier for this provider implementation (e.g., "openai", "gemini", "local").
     */
    String getProviderId();

    /**
     * Checks if this provider is properly configured and available to handle requests.
     */
    boolean isAvailable();

    /**
     * Sends the prompt with conversation history and system instructions to the AI model.
     *
     * @param systemInstruction System instructions containing persona and grounded DB context
     * @param history Prior message turns (if any)
     * @param userMessage Latest user query
     * @return AI assistant reply as markdown text
     */
    String chat(String systemInstruction, List<ChatMessageDto> history, String userMessage);
}
