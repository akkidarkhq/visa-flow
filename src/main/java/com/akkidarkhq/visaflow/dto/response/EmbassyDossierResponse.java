package com.akkidarkhq.visaflow.dto.response;

import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbassyDossierResponse {
    private UUID applicationId;
    private String referenceNumber;
    private ApplicationStatus status;
    private UUID orderId;
    private String orderReference;

    // Traveller Details
    private UUID travellerId;
    private String travellerFullName;
    private String passportNumber;
    private String nationality;

    // Visa Details
    private Long visaTypeId;
    private String visaTypeName;
    private String countryName;
    private LocalDate travelStartDate;
    private LocalDate travelEndDate;

    // Verified documents packaged for embassy submission
    private List<ApprovedDocumentSummary> approvedDocuments;

    private OffsetDateTime submittedAt;
    private String submissionMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovedDocumentSummary {
        private UUID documentId;
        private Long requirementId;
        private String requirementTitle;
        private String documentCategory;
        private String fileName;
        private String mimeType;
        private Long fileSizeBytes;
        private String s3FileKey;
        private String downloadEndpoint;
    }
}
