package com.akkidarkhq.visaflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravellerResponseDto {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String passportNumber;
    private LocalDate passportExpiry;
    private LocalDate dateOfBirth;
    private String nationality;
    private String gender;
    private OffsetDateTime createdAt;
}
