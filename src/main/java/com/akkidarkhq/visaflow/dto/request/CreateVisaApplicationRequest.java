package com.akkidarkhq.visaflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVisaApplicationRequest {

    @NotNull(message = "travellerId is required")
    @Schema(description = "UUID of the TravellerProfile", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID travellerId;

    @NotNull(message = "visaTypeId is required")
    @Schema(description = "ID of the VisaType", example = "1")
    private Long visaTypeId;

    @NotNull(message = "travelStartDate is required")
    @Schema(description = "Intended date of departure/arrival in destination (YYYY-MM-DD)", example = "2026-08-15")
    private LocalDate travelStartDate;

    @NotNull(message = "travelEndDate is required")
    @Schema(description = "Intended date of return (YYYY-MM-DD)", example = "2026-08-25")
    private LocalDate travelEndDate;
}
