package com.akkidarkhq.visaflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryResponse {
    private Long id;
    private String isoCode;
    private String name;
    private String flagIconUrl;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}
