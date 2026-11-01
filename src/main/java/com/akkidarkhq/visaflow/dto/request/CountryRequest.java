package com.akkidarkhq.visaflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryRequest {

    @NotBlank(message = "ISO code is required")
    @Size(min = 2, max = 3, message = "ISO code must be 2 or 3 letters")
    @Pattern(regexp = "^[A-Za-z]{2,3}$", message = "ISO code must contain only letters")
    private String isoCode;

    @NotBlank(message = "Country name is required")
    @Size(max = 100, message = "Country name must be at most 100 characters")
    private String name;

    private String flagIconUrl;

    @Builder.Default
    private Boolean isActive = true;
}
