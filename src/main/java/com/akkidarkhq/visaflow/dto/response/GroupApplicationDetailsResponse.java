package com.akkidarkhq.visaflow.dto.response;

import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupApplicationDetailsResponse {

    private String groupReference;
    private Long visaTypeId;
    private String visaTypeName;
    private String countryName;
    private LocalDate travelStartDate;
    private LocalDate travelEndDate;
    private BigDecimal serviceFee;
    private BigDecimal embassyFee;
    private BigDecimal feePerPerson;
    private BigDecimal totalEstimatedCost;
    private String currency;
    private int totalTravellers;
    private boolean isReadyForCheckout;
    private List<GroupTravellerApplicationItem> travellers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupTravellerApplicationItem {
        private UUID applicationId;
        private String referenceNumber;
        private ApplicationStatus status;
        private LocalDate travelStartDate;
        private LocalDate travelEndDate;
        private OffsetDateTime createdAt;

        // Traveller details
        private UUID travellerId;
        private String firstName;
        private String lastName;
        private String fullName;
        private String passportNumber;
        private LocalDate passportExpiry;
        private LocalDate dateOfBirth;
        private String nationality;
        private String gender;

        // Order & Payment link
        private UUID orderItemId;
        private String orderReference;
        private boolean isPaid;

        // Documents summary
        private DocumentsSummary documentsSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentsSummary {
        private int totalMandatoryRequired;
        private int totalUploaded;
        private int totalApproved;
        private int totalUnderReview;
        private int totalCorrectionRequested;
        private int totalRejected;
    }
}
