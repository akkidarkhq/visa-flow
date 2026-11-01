package com.akkidarkhq.visaflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisaTypeResponse {
    private Long id;
    private Long countryId;
    private String countryName;
    private String countryIsoCode;
    private String name;
    private Integer processingTimeDays;
    private BigDecimal serviceFee;
    private BigDecimal embassyFee;
    private BigDecimal totalFee;
    private String currency;
    private String description;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}
