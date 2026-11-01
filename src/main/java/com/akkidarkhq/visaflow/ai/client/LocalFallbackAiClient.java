package com.akkidarkhq.visaflow.ai.client;

import com.akkidarkhq.visaflow.ai.dto.ChatMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Built-in local rule & knowledge assistant provider.
 * Runs instantly without requiring external internet or API keys.
 * Serves responses grounded directly in VisaFlow's database context.
 */
@Slf4j
@Component
public class LocalFallbackAiClient implements AiClient {

    @Override
    public String getProviderId() {
        return "visaflow:local-rag";
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available
    }

    @Override
    public String chat(String systemInstruction, List<ChatMessageDto> history, String userMessage) {
        log.info("Processing query using LocalFallbackAiClient");
        String lower = userMessage.toLowerCase().trim();

        // Check for greetings
        if (lower.matches("^(hi|hello|hey|greetings|good morning|good evening|good afternoon).*")) {
            return "👋 Hello! I am **VisaFlow AI Assistant**.\n\n" +
                    "I can help you with:\n" +
                    "- 📋 **Visa Requirements & Document Checklists** (e.g., *'What documents are needed for France?'*)\n" +
                    "- 💰 **Visa Fees & Processing Times** (e.g., *'How much does a Tourist Visa to Japan cost?'*)\n" +
                    "- 🔍 **Track Application Status** (e.g., *'Check status for GRP-2026-X8K9M2'* or *'VIS-...' *)\n\n" +
                    "Which country or visa application would you like help with today?";
        }

        // If systemInstruction contains grounded DB context, synthesize a clean response from it
        if (systemInstruction != null && systemInstruction.contains("--- VISAFLOW DATABASE CONTEXT ---")) {
            int ctxStart = systemInstruction.indexOf("--- VISAFLOW DATABASE CONTEXT ---");
            String contextBlock = systemInstruction.substring(ctxStart + "--- VISAFLOW DATABASE CONTEXT ---".length()).trim();

            return "Here is the verified information from our official **VisaFlow** database:\n\n" +
                    contextBlock + "\n\n" +
                    "> 💡 *Tip: Make sure all uploaded documents are clearly legible and valid for at least 6 months beyond your travel date.*";
        }

        // Fallback guidance when no specific database entity was matched
        return "I couldn't locate specific records for that query in VisaFlow's active catalog.\n\n" +
                "**You can ask me about:**\n" +
                "1. **Destination Country Requirements**: Specify the country (e.g. *'Documents for France'* or *'Japan visa fees'*).\n" +
                "2. **Application Status**: Provide your tracking reference code (e.g. `GRP-...` or `VIS-...`).\n" +
                "3. **General Document Rules**: Photo requirements, bank statement validity, or passport rules.\n\n" +
                "How else can I assist your travel visa process?";
    }
}
