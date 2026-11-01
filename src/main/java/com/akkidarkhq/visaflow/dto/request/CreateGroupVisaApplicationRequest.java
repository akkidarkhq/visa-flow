package com.akkidarkhq.visaflow.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupVisaApplicationRequest {

    @NotNull(message = "visaTypeId is required")
    private Long visaTypeId;

    @NotEmpty(message = "travellerIds list cannot be empty")
    private List<UUID> travellerIds;
}
