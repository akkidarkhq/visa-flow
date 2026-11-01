package com.akkidarkhq.visaflow.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TravellerProfileRequest {

    private UUID userId;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Passport number is required")
    @Size(max = 50)
    private String passportNumber;

    @Future(message = "Passport expiry must be in the future")
    private LocalDate passportExpiry;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Nationality is required")
    private String nationality;

    private String gender;
}