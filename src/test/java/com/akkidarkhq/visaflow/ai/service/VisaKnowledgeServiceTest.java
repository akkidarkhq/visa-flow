package com.akkidarkhq.visaflow.ai.service;

import com.akkidarkhq.visaflow.ai.dto.ChatRequest;
import com.akkidarkhq.visaflow.model.ChecklistRequirement;
import com.akkidarkhq.visaflow.model.Country;
import com.akkidarkhq.visaflow.model.VisaType;
import com.akkidarkhq.visaflow.repository.ChecklistRequirementRepository;
import com.akkidarkhq.visaflow.repository.CountryRepository;
import com.akkidarkhq.visaflow.repository.VisaApplicationRepository;
import com.akkidarkhq.visaflow.repository.VisaTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisaKnowledgeServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private VisaTypeRepository visaTypeRepository;

    @Mock
    private ChecklistRequirementRepository checklistRequirementRepository;

    @Mock
    private VisaApplicationRepository visaApplicationRepository;

    @InjectMocks
    private VisaKnowledgeService visaKnowledgeService;

    private Country france;
    private VisaType touristVisa;
    private ChecklistRequirement passportReq;

    @BeforeEach
    void setUp() {
        france = Country.builder()
                .id(1L)
                .name("France")
                .isoCode("FRA")
                .isActive(true)
                .build();

        touristVisa = VisaType.builder()
                .id(10L)
                .country(france)
                .name("Tourist Visa")
                .processingTimeDays(7)
                .serviceFee(new BigDecimal("1500.00"))
                .embassyFee(new BigDecimal("7500.00"))
                .currency("INR")
                .description("Schengen short-stay tourist visa")
                .isActive(true)
                .build();

        passportReq = ChecklistRequirement.builder()
                .id(100L)
                .visaType(touristVisa)
                .title("Original Passport")
                .documentCategory("Identity")
                .isMandatory(true)
                .allowedFormats("PDF,JPG")
                .maxFileSizeMb(5)
                .displayOrder(1)
                .build();
    }

    @Test
    void testBuildGroundedContext_withCountryNameInMessage() {
        when(countryRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(france));
        when(visaTypeRepository.findByCountryIdAndIsActiveTrue(1L)).thenReturn(List.of(touristVisa));
        when(checklistRequirementRepository.findByVisaTypeIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(passportReq));

        ChatRequest request = ChatRequest.builder()
                .message("What are the requirements for France?")
                .build();

        VisaKnowledgeService.GroundedContext context = visaKnowledgeService.buildGroundedContext(request);

        assertNotNull(context);
        assertTrue(context.isHasData());
        assertEquals("France", context.getMatchedCountry().getName());
        assertTrue(context.getFormattedContext().contains("France"));
        assertTrue(context.getFormattedContext().contains("Tourist Visa"));
        assertTrue(context.getFormattedContext().contains("Original Passport"));
        assertTrue(context.getFormattedContext().contains("7500.00 INR"));
    }

    @Test
    void testBuildSystemPrompt_containsGroundedContext() {
        VisaKnowledgeService.GroundedContext context = VisaKnowledgeService.GroundedContext.builder()
                .formattedContext("France Tourist Visa: Embassy Fee 7500 INR")
                .hasData(true)
                .build();

        String prompt = visaKnowledgeService.buildSystemPrompt(context);

        assertNotNull(prompt);
        assertTrue(prompt.contains("VisaFlow AI Assistant"));
        assertTrue(prompt.contains("--- VISAFLOW DATABASE CONTEXT ---"));
        assertTrue(prompt.contains("France Tourist Visa"));
    }
}
