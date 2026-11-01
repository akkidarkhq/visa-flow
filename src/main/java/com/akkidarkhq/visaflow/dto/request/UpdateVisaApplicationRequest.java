package com.akkidarkhq.visaflow.dto.request;

import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVisaApplicationRequest {

    @NotNull(message = "status is required")
    private ApplicationStatus status;

    private String notes;
}
