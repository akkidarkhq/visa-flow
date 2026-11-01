package com.akkidarkhq.visaflow.dto.request;

import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDocumentRequest {

    @NotNull(message = "status is required")
    private DocumentStatus status; // APPROVED, REJECTED, CORRECTION_REQUESTED

    private String adminRemark;
}
