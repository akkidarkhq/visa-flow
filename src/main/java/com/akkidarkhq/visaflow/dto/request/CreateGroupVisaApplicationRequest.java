package com.akkidarkhq.visaflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupVisaApplicationRequest {

    @NotNull(message = "visaTypeId is required")
    @Schema(description = "ID of the VisaType for the group", example = "1")
    private Long visaTypeId;

    @NotNull(message = "travelStartDate is required")
    @Schema(description = "Intended trip departure date (YYYY-MM-DD)", example = "2026-08-15")
    private LocalDate travelStartDate;

    @NotNull(message = "travelEndDate is required")
    @Schema(description = "Intended trip return date (YYYY-MM-DD)", example = "2026-08-25")
    private LocalDate travelEndDate;

    @NotEmpty(message = "travellerIds list cannot be empty")
    @Schema(description = "List of companion traveller UUIDs joining this group trip")
    private List<UUID> travellerIds;
}
