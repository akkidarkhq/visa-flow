package com.akkidarkhq.visaflow.dto.response;

import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisaApplicationResponse {
    private UUID id;
    private UUID orderItemId;
    private String referenceNumber;
    private String groupReference;
    private UUID travellerId;
    private String travellerFullName;
    private String passportNumber;
    private Long visaTypeId;
    private String visaTypeName;
    private String countryName;
    private LocalDate travelStartDate;
    private LocalDate travelEndDate;
    private ApplicationStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
