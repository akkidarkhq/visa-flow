package com.akkidarkhq.visaflow.ai.service;

import com.akkidarkhq.visaflow.ai.dto.ChatRequest;
import com.akkidarkhq.visaflow.model.ChecklistRequirement;
import com.akkidarkhq.visaflow.model.Country;
import com.akkidarkhq.visaflow.model.VisaApplication;
import com.akkidarkhq.visaflow.model.VisaType;
import com.akkidarkhq.visaflow.repository.ChecklistRequirementRepository;
import com.akkidarkhq.visaflow.repository.CountryRepository;
import com.akkidarkhq.visaflow.repository.VisaApplicationRepository;
import com.akkidarkhq.visaflow.repository.VisaTypeRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieves and formats real-time domain facts from VisaFlow's database (Countries,
 * Visa Types, Checklist Requirements, and Application Statuses) to ground the AI responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisaKnowledgeService {

    private final CountryRepository countryRepository;
    private final VisaTypeRepository visaTypeRepository;
    private final ChecklistRequirementRepository checklistRequirementRepository;
    private final VisaApplicationRepository visaApplicationRepository;

    private static final Pattern REF_PATTERN = Pattern.compile("\\b(GRP-[A-Za-z0-9\\-]+|VIS-[A-Za-z0-9\\-]+)\\b", Pattern.CASE_INSENSITIVE);

    @Data
    @Builder
    public static class GroundedContext {
        private String formattedContext;
        private boolean hasData;
        private Country matchedCountry;
        private List<String> followUps;
    }

    /**
     * Extracts grounding context for a chat request.
     */
    @Transactional(readOnly = true)
    public GroundedContext buildGroundedContext(ChatRequest request) {
        StringBuilder contextBuilder = new StringBuilder();
        boolean hasData = false;
        Country matchedCountry = null;
        List<String> followUps = new ArrayList<>();

        String message = request.getMessage() != null ? request.getMessage() : "";

        // 1. Check for Application or Group Tracking references
        String trackingRef = extractReference(request, message);
        if (trackingRef != null) {
            String appInfo = lookupApplicationOrGroup(trackingRef);
            if (appInfo != null) {
                contextBuilder.append("### APPLICATION / GROUP TRACKING INFORMATION\n")
                        .append(appInfo).append("\n\n");
                hasData = true;
                followUps.add("What is the next step for my application?");
                followUps.add("Can I upload missing documents now?");
            }
        }

        // 2. Resolve Country & Visa Types Context
        matchedCountry = resolveCountry(request, message);
        if (matchedCountry != null) {
            hasData = true;
            contextBuilder.append("### DESTINATION COUNTRY & VISA REQUIREMENTS: ")
                    .append(matchedCountry.getName())
                    .append(" (ISO: ").append(matchedCountry.getIsoCode()).append(")\n");

            List<VisaType> visaTypes = visaTypeRepository.findByCountryIdAndIsActiveTrue(matchedCountry.getId());
            if (visaTypes.isEmpty()) {
                contextBuilder.append("Currently, there are no active visa products listed for this country in VisaFlow.\n\n");
            } else {
                for (VisaType vt : visaTypes) {
                    contextBuilder.append("• **Visa Type**: ").append(vt.getName()).append(" (ID: ").append(vt.getId()).append(")\n")
                            .append("  - Processing Time: ").append(vt.getProcessingTimeDays()).append(" business days\n")
                            .append("  - Embassy Fee: ").append(vt.getEmbassyFee()).append(" ").append(vt.getCurrency()).append("\n")
                            .append("  - VisaFlow Service Fee: ").append(vt.getServiceFee()).append(" ").append(vt.getCurrency()).append("\n")
                            .append("  - Total Fee: ").append(vt.getEmbassyFee().add(vt.getServiceFee())).append(" ").append(vt.getCurrency()).append("\n");

                    if (vt.getDescription() != null && !vt.getDescription().isBlank()) {
                        contextBuilder.append("  - Details: ").append(vt.getDescription()).append("\n");
                    }

                    List<ChecklistRequirement> checklist = checklistRequirementRepository
                            .findByVisaTypeIdOrderByDisplayOrderAsc(vt.getId());

                    if (!checklist.isEmpty()) {
                        contextBuilder.append("  - **Document Checklist**:\n");
                        int count = 1;
                        for (ChecklistRequirement req : checklist) {
                            contextBuilder.append("    ").append(count++).append(". ")
                                    .append(req.getTitle())
                                    .append(req.getIsMandatory() ? " *(Mandatory)*" : " *(Optional)*")
                                    .append(" [Category: ").append(req.getDocumentCategory())
                                    .append(", Formats: ").append(req.getAllowedFormats())
                                    .append(", Max Size: ").append(req.getMaxFileSizeMb()).append("MB]");
                            if (req.getDescription() != null && !req.getDescription().isBlank()) {
                                contextBuilder.append(" - ").append(req.getDescription());
                            }
                            contextBuilder.append("\n");
                        }
                    }
                    contextBuilder.append("\n");
                }
            }

            if (followUps.isEmpty()) {
                followUps.add("What are the photo specifications for " + matchedCountry.getName() + "?");
                followUps.add("How long does processing take for " + matchedCountry.getName() + "?");
                followUps.add("What are the total fees for " + matchedCountry.getName() + "?");
            }
        }

        // 3. If no specific country or reference, provide active countries catalog
        if (!hasData) {
            List<Country> activeCountries = countryRepository.findByIsActiveTrueOrderByNameAsc();
            if (!activeCountries.isEmpty()) {
                contextBuilder.append("### SUPPORTED DESTINATION COUNTRIES IN VISAFLOW\n")
                        .append("Active destinations currently available for visa processing: ");
                List<String> names = activeCountries.stream().map(c -> c.getName() + " (" + c.getIsoCode() + ")").toList();
                contextBuilder.append(String.join(", ", names)).append(".\n\n");
                hasData = true;
            }

            followUps.add("What documents are required for France?");
            followUps.add("How does VisaFlow group travel visa application work?");
            followUps.add("What is the passport validity requirement for travel?");
        }

        return GroundedContext.builder()
                .formattedContext(contextBuilder.toString().trim())
                .hasData(hasData)
                .matchedCountry(matchedCountry)
                .followUps(followUps)
                .build();
    }

    /**
     * Builds the complete system prompt incorporating persona and grounded DB facts.
     */
    public String buildSystemPrompt(GroundedContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the official VisaFlow AI Assistant, an expert visa consultant for the VisaFlow platform.\n");
        sb.append("Your mission is to provide accurate, transparent, and structured visa guidance to travelers and clients.\n\n");
        sb.append("GUIDELINES:\n");
        sb.append("1. ALWAYS rely on the verified VisaFlow Database Context provided below as your primary source of truth.\n");
        sb.append("2. If fees, processing times, or required documents are present in the context, quote them accurately.\n");
        sb.append("3. Format responses using clean markdown: bullet points, numbered checklist steps, bold headings, and tables if helpful.\n");
        sb.append("4. Remind travelers of standard travel practices when relevant (e.g., passports must have at least 6 months validity).\n");
        sb.append("5. Maintain a professional, polite, and reassuring tone.\n\n");

        if (context.isHasData() && !context.getFormattedContext().isBlank()) {
            sb.append("--- VISAFLOW DATABASE CONTEXT ---\n");
            sb.append(context.getFormattedContext()).append("\n");
            sb.append("--- END OF DATABASE CONTEXT ---\n");
        }

        return sb.toString();
    }

    private String extractReference(ChatRequest request, String message) {
        if (request.getReferenceNumber() != null && !request.getReferenceNumber().isBlank()) {
            return request.getReferenceNumber().trim().toUpperCase();
        }
        Matcher matcher = REF_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    private String lookupApplicationOrGroup(String ref) {
        if (ref.startsWith("GRP-")) {
            List<VisaApplication> groupApps = visaApplicationRepository.findByGroupReference(ref);
            if (!groupApps.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Group Reference: **").append(ref).append("**\n")
                        .append("Total Applicants in Group: ").append(groupApps.size()).append("\n")
                        .append("Applications:\n");
                for (VisaApplication app : groupApps) {
                    String name = app.getTraveller() != null ? app.getTraveller().getFirstName() + " " + app.getTraveller().getLastName() : "Applicant";
                    String visaName = app.getVisaType() != null ? app.getVisaType().getName() : "Visa";
                    sb.append("  - Ref: `").append(app.getReferenceNumber()).append("` | Traveller: ").append(name)
                            .append(" | Visa: ").append(visaName)
                            .append(" | Status: **").append(app.getStatus()).append("**\n");
                }
                return sb.toString();
            }
        } else if (ref.startsWith("VIS-")) {
            Optional<VisaApplication> appOpt = visaApplicationRepository.findByReferenceNumber(ref);
            if (appOpt.isPresent()) {
                VisaApplication app = appOpt.get();
                String name = app.getTraveller() != null ? app.getTraveller().getFirstName() + " " + app.getTraveller().getLastName() : "Applicant";
                String country = (app.getVisaType() != null && app.getVisaType().getCountry() != null) ? app.getVisaType().getCountry().getName() : "Destination";
                String visaName = app.getVisaType() != null ? app.getVisaType().getName() : "Visa";

                return "Application Reference: `**" + app.getReferenceNumber() + "**`\n" +
                        "- Destination: " + country + "\n" +
                        "- Visa Type: " + visaName + "\n" +
                        "- Traveller: " + name + "\n" +
                        "- Current Status: **" + app.getStatus() + "**\n" +
                        (app.getGroupReference() != null ? "- Group Reference: `" + app.getGroupReference() + "`\n" : "");
            }
        }
        return null;
    }

    private Country resolveCountry(ChatRequest request, String message) {
        // 1. Direct request parameter
        if (request.getCountryCode() != null && !request.getCountryCode().isBlank()) {
            String code = request.getCountryCode().trim();
            Optional<Country> countryOpt = countryRepository.findByIsoCodeIgnoreCase(code);
            if (countryOpt.isPresent()) return countryOpt.get();
            countryOpt = countryRepository.findByNameIgnoreCase(code);
            if (countryOpt.isPresent()) return countryOpt.get();
        }

        // 2. By visa type ID parameter
        if (request.getVisaTypeId() != null) {
            Optional<VisaType> vtOpt = visaTypeRepository.findById(request.getVisaTypeId());
            if (vtOpt.isPresent() && vtOpt.get().getCountry() != null) {
                return vtOpt.get().getCountry();
            }
        }

        // 3. Scan message text for active country names or ISO codes
        List<Country> activeCountries = countryRepository.findByIsActiveTrueOrderByNameAsc();
        String lowerMsg = message.toLowerCase();

        for (Country country : activeCountries) {
            if (lowerMsg.contains(country.getName().toLowerCase())) {
                return country;
            }
            if (country.getIsoCode() != null && country.getIsoCode().length() >= 2) {
                Pattern isoPattern = Pattern.compile("\\b" + Pattern.quote(country.getIsoCode().toLowerCase()) + "\\b");
                if (isoPattern.matcher(lowerMsg).find()) {
                    return country;
                }
            }
        }

        return null;
    }
}
