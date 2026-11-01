package com.akkidarkhq.visaflow.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory and registry to resolve the active AI client provider based on configuration.
 */
@Slf4j
@Component
public class AiClientFactory {

    private final OpenAiClient openAiClient;
    private final GeminiClient geminiClient;
    private final LocalFallbackAiClient localFallbackAiClient;
    private final String configuredProvider;

    public AiClientFactory(
            OpenAiClient openAiClient,
            GeminiClient geminiClient,
            LocalFallbackAiClient localFallbackAiClient,
            @Value("${visaflow.ai.provider:auto}") String configuredProvider) {
        this.openAiClient = openAiClient;
        this.geminiClient = geminiClient;
        this.localFallbackAiClient = localFallbackAiClient;
        this.configuredProvider = configuredProvider != null ? configuredProvider.trim().toLowerCase() : "auto";
    }

    /**
     * Resolves the primary client to use for incoming chat requests.
     */
    public AiClient getActiveClient() {
        return switch (configuredProvider) {
            case "openai" -> {
                if (openAiClient.isAvailable()) {
                    yield openAiClient;
                }
                log.warn("OpenAI requested in config but API key not provided; falling back to local provider");
                yield localFallbackAiClient;
            }
            case "gemini" -> {
                if (geminiClient.isAvailable()) {
                    yield geminiClient;
                }
                log.warn("Gemini requested in config but API key not provided; falling back to local provider");
                yield localFallbackAiClient;
            }
            case "local" -> localFallbackAiClient;
            default -> {
                // Auto mode: OpenAI -> Gemini -> Local
                if (openAiClient.isAvailable()) {
                    log.info("Auto-selected OpenAI as active AI provider");
                    yield openAiClient;
                }
                if (geminiClient.isAvailable()) {
                    log.info("Auto-selected Gemini as active AI provider");
                    yield geminiClient;
                }
                log.info("No external LLM API key detected; using VisaFlow local RAG provider");
                yield localFallbackAiClient;
            }
        };
    }

    /**
     * Fallback client in case an external API provider encounters rate limits or errors.
     */
    public AiClient getFallbackClient() {
        return localFallbackAiClient;
    }

    /**
     * Lists all registered provider IDs and their availability.
     */
    public List<ProviderStatus> getProvidersStatus() {
        return List.of(
                new ProviderStatus(openAiClient.getProviderId(), openAiClient.isAvailable()),
                new ProviderStatus(geminiClient.getProviderId(), geminiClient.isAvailable()),
                new ProviderStatus(localFallbackAiClient.getProviderId(), localFallbackAiClient.isAvailable())
        );
    }

    public record ProviderStatus(String providerId, boolean available) {}
}
