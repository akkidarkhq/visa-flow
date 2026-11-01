package com.akkidarkhq.visaflow.controller;

import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.request.TravellerProfileRequest;
import com.akkidarkhq.visaflow.dto.response.TravellerResponseDto;
import com.akkidarkhq.visaflow.service.TravellerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing individual traveller profiles associated with a user and visa applications.
 */
@RestController
@RequestMapping("/api/travellers")
@RequiredArgsConstructor
@Tag(name = "Traveller Profiles", description = "Endpoints for creating and managing traveller biodata and passport details")
public class TravellerProfileController {

    private final TravellerProfileService travellerProfileService;

    /**
     * Creates a traveller profile for a user.
     *
     * @param request Traveller personal and passport data
     * @return Created TravellerResponseDto
     */
    @PostMapping
    @Operation(summary = "Create traveller profile",
            description = "Creates a persistent traveller profile with passport details under the specified user account.")
    public ResponseEntity<ApiResponse<TravellerResponseDto>> createTraveller(@Valid @RequestBody TravellerProfileRequest request) {
        TravellerResponseDto response = travellerProfileService.createTraveller(request);
        return new ResponseEntity<>(
                ApiResponse.success("Traveller profile created successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Retrieves the traveller profile for a given Visa Application.
     *
     * @param applicationId UUID of the Visa Application
     * @return Traveller profile details
     */
    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Get traveller by visa application ID",
            description = "Retrieves the full traveller profile and passport details associated with a specific visa application.")
    public ResponseEntity<ApiResponse<TravellerResponseDto>> getTravellerByApplication(
            @Parameter(description = "Visa Application UUID")
            @PathVariable UUID applicationId) {
        TravellerResponseDto response = travellerProfileService.getTravellerByApplicationId(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves a traveller profile by ID.
     *
     * @param id Traveller profile UUID
     * @return Traveller profile details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get traveller by ID", description = "Retrieves a specific traveller profile by its UUID.")
    public ResponseEntity<ApiResponse<TravellerResponseDto>> getTravellerById(
            @Parameter(description = "Traveller Profile UUID")
            @PathVariable UUID id) {
        TravellerResponseDto response = travellerProfileService.getTravellerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves all traveller profiles created under a user account.
     *
     * @param userId User UUID
     * @return List of traveller profiles
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get travellers by User ID",
            description = "Retrieves all saved traveller profiles belonging to the specified user.")
    public ResponseEntity<ApiResponse<List<TravellerResponseDto>>> getTravellersByUser(
            @Parameter(description = "User UUID")
            @PathVariable UUID userId) {
        List<TravellerResponseDto> response = travellerProfileService.getTravellersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
