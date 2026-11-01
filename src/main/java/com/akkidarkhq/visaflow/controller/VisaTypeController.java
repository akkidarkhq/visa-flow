package com.akkidarkhq.visaflow.controller;

import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.request.VisaTypeRequest;
import com.akkidarkhq.visaflow.dto.response.VisaTypeResponse;
import com.akkidarkhq.visaflow.service.VisaTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing Visa Types (Tourist, Business, Transit, etc.) and fee schedules.
 */
@RestController
@RequestMapping("/api/visa-types")
@RequiredArgsConstructor
@Tag(name = "Visa Types", description = "Endpoints for managing visa categories, processing times, service and embassy fees")
public class VisaTypeController {

    private final VisaTypeService visaTypeService;

    /**
     * Unified creation endpoint for Visa Types.
     * Accepts either a single object or an array of objects.
     *
     * @param requests Single or list of VisaTypeRequest objects
     * @return List of created visa types
     */
    @PostMapping
    @Operation(summary = "Create visa type(s)",
            description = "Creates one or more visa types associated with a country, including embassy and service fee schedules.")
    public ResponseEntity<ApiResponse<List<VisaTypeResponse>>> createVisaTypes(
            @Valid @RequestBody List<VisaTypeRequest> requests) {
        List<VisaTypeResponse> response = visaTypeService.createVisaTypesBulk(requests);
        return new ResponseEntity<>(
                ApiResponse.success("Visa type(s) created successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Retrieves all active visa types.
     *
     * @return List of active visa types
     */
    @GetMapping
    @Operation(summary = "Get all active visa types", description = "Retrieves all active visa categories across all countries.")
    public ResponseEntity<ApiResponse<List<VisaTypeResponse>>> getAllVisaTypes() {
        List<VisaTypeResponse> response = visaTypeService.getAllActiveVisaTypes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves a visa type by ID.
     *
     * @param id Visa type ID
     * @return Visa type details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get visa type by ID", description = "Retrieves specific visa type details by ID.")
    public ResponseEntity<ApiResponse<VisaTypeResponse>> getVisaTypeById(
            @Parameter(description = "Visa Type ID")
            @PathVariable Long id) {
        VisaTypeResponse response = visaTypeService.getVisaTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves all visa types for a specific country ID.
     *
     * @param countryId Country ID
     * @return List of visa types available for that country
     */
    @GetMapping("/country/{countryId}")
    @Operation(summary = "Get visa types by Country ID", description = "Retrieves all visa options available for a specific destination country.")
    public ResponseEntity<ApiResponse<List<VisaTypeResponse>>> getVisaTypesByCountry(
            @Parameter(description = "Country ID")
            @PathVariable Long countryId) {
        List<VisaTypeResponse> response = visaTypeService.getVisaTypesByCountry(countryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
