package com.akkidarkhq.visaflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVisaApplicationRequest {

    @NotNull(message = "travellerId is required")
    private UUID travellerId;

    @NotNull(message = "visaTypeId is required")
    private Long visaTypeId;
}
