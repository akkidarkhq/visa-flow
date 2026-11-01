package com.akkidarkhq.visaflow.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisaTypeRequest {

    @NotNull(message = "countryId is required")
    private Long countryId;

    @NotBlank(message = "Visa type name is required")
    private String name;

    @NotNull(message = "processingTimeDays is required")
    @Min(value = 1, message = "processingTimeDays must be at least 1")
    private Integer processingTimeDays;

    @NotNull(message = "serviceFee is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "serviceFee cannot be negative")
    private BigDecimal serviceFee;

    @NotNull(message = "embassyFee is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "embassyFee cannot be negative")
    private BigDecimal embassyFee;

    private String currency;

    private String description;

    @Builder.Default
    private Boolean isActive = true;
}
