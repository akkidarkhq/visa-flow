package com.akkidarkhq.visaflow.ai.service;

import com.akkidarkhq.visaflow.ai.client.AiClient;
import com.akkidarkhq.visaflow.ai.client.AiClientFactory;
import com.akkidarkhq.visaflow.ai.dto.ChatRequest;
import com.akkidarkhq.visaflow.ai.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private VisaKnowledgeService visaKnowledgeService;

    @Mock
    private AiClientFactory aiClientFactory;

    @Mock
    private AiClient primaryAiClient;

    @Mock
    private AiClient fallbackAiClient;

    @InjectMocks
    private AiChatService aiChatService;

    @Test
    void testProcessChat_successfulPrimaryClient() {
        ChatRequest request = ChatRequest.builder()
                .message("Documents for France")
                .build();

        VisaKnowledgeService.GroundedContext mockContext = VisaKnowledgeService.GroundedContext.builder()
                .hasData(true)
                .formattedContext("France Tourist Visa")
                .followUps(List.of("What is the fee?"))
                .build();

        when(visaKnowledgeService.buildGroundedContext(request)).thenReturn(mockContext);
        when(visaKnowledgeService.buildSystemPrompt(mockContext)).thenReturn("System Instructions");
        when(aiClientFactory.getActiveClient()).thenReturn(primaryAiClient);
        when(primaryAiClient.getProviderId()).thenReturn("openai:gpt-4o-mini");
        when(primaryAiClient.chat(anyString(), any(), anyString())).thenReturn("You need a passport and photo.");

        ChatResponse response = aiChatService.processChat(request);

        assertNotNull(response);
        assertEquals("You need a passport and photo.", response.getReply());
        assertEquals("openai:gpt-4o-mini", response.getProvider());
        assertTrue(response.isContextGrounding());
        assertEquals(1, response.getSuggestedFollowUps().size());
        verify(primaryAiClient, times(1)).chat(anyString(), any(), anyString());
    }

    @Test
    void testProcessChat_primaryClientFails_gracefulFallback() {
        ChatRequest request = ChatRequest.builder()
                .message("Documents for France")
                .build();

        VisaKnowledgeService.GroundedContext mockContext = VisaKnowledgeService.GroundedContext.builder()
                .hasData(true)
                .formattedContext("France Tourist Visa")
                .build();

        when(visaKnowledgeService.buildGroundedContext(request)).thenReturn(mockContext);
        when(visaKnowledgeService.buildSystemPrompt(mockContext)).thenReturn("System Instructions");
        when(aiClientFactory.getActiveClient()).thenReturn(primaryAiClient);
        when(primaryAiClient.getProviderId()).thenReturn("openai:gpt-4o-mini");
        when(primaryAiClient.chat(anyString(), any(), anyString())).thenThrow(new RuntimeException("Rate limit"));

        when(aiClientFactory.getFallbackClient()).thenReturn(fallbackAiClient);
        when(fallbackAiClient.getProviderId()).thenReturn("visaflow:local-rag");
        when(fallbackAiClient.chat(anyString(), any(), anyString())).thenReturn("Fallback answer from DB context.");

        ChatResponse response = aiChatService.processChat(request);

        assertNotNull(response);
        assertEquals("Fallback answer from DB context.", response.getReply());
        assertTrue(response.getProvider().contains("fallback"));
        verify(fallbackAiClient, times(1)).chat(anyString(), any(), anyString());
    }
}
