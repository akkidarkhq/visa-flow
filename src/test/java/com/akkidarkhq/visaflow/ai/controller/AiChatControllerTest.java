package com.akkidarkhq.visaflow.ai.controller;

import com.akkidarkhq.visaflow.ai.client.AiClient;
import com.akkidarkhq.visaflow.ai.client.AiClientFactory;
import com.akkidarkhq.visaflow.ai.dto.ChatRequest;
import com.akkidarkhq.visaflow.ai.dto.ChatResponse;
import com.akkidarkhq.visaflow.ai.service.AiChatService;
import com.akkidarkhq.visaflow.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private AiClientFactory aiClientFactory;

    @Mock
    private AiClient activeClient;

    @InjectMocks
    private AiChatController aiChatController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(aiChatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testChatEndpoint_success() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("What are France visa requirements?")
                .build();

        ChatResponse response = ChatResponse.builder()
                .reply("Passport and photo required.")
                .provider("visaflow:local-rag")
                .contextGrounding(true)
                .suggestedFollowUps(List.of("What is the fee?"))
                .timestamp(OffsetDateTime.now())
                .build();

        when(aiChatService.processChat(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").value("Passport and photo required."))
                .andExpect(jsonPath("$.data.provider").value("visaflow:local-rag"))
                .andExpect(jsonPath("$.data.contextGrounding").value(true));
    }

    @Test
    void testChatEndpoint_blankMessage_validationFailure() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetProvidersEndpoint_success() throws Exception {
        when(aiClientFactory.getActiveClient()).thenReturn(activeClient);
        when(activeClient.getProviderId()).thenReturn("visaflow:local-rag");
        when(aiClientFactory.getProvidersStatus()).thenReturn(List.of(
                new AiClientFactory.ProviderStatus("visaflow:local-rag", true)
        ));

        mockMvc.perform(get("/api/chat/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeProvider").value("visaflow:local-rag"));
    }
}
