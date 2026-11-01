package com.akkidarkhq.visaflow.controller;

import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.request.CountryRequest;
import com.akkidarkhq.visaflow.dto.response.CountryResponse;
import com.akkidarkhq.visaflow.service.CountryService;
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
 * Controller for managing supported destination countries and ISO codes.
 */
@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
@Tag(name = "Countries", description = "Endpoints for managing destination countries, ISO-2 codes, and active statuses")
public class CountryController {

    private final CountryService countryService;

    /**
     * Unified creation endpoint for Countries.
     * Accepts either a single object or an array of objects.
     *
     * @param requests Single or list of CountryRequest objects
     * @return List of created countries
     */
    @PostMapping
    @Operation(summary = "Create country / countries",
            description = "Creates one or more supported destination countries with ISO code, currency, and continent information.")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> createCountries(
            @Valid @RequestBody List<CountryRequest> requests) {
        List<CountryResponse> response = countryService.createCountries(requests);
        return new ResponseEntity<>(
                ApiResponse.success("Country / Countries created successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Retrieves all active countries.
     *
     * @return List of active countries
     */
    @GetMapping
    @Operation(summary = "Get all active countries", description = "Retrieves all destination countries currently enabled for visa applications.")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getAllCountries() {
        List<CountryResponse> response = countryService.getAllActiveCountries();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves a country by ID.
     *
     * @param id Country ID
     * @return Country details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get country by ID", description = "Retrieves country details by its internal ID.")
    public ResponseEntity<ApiResponse<CountryResponse>> getCountryById(
            @Parameter(description = "Country ID")
            @PathVariable Long id) {
        CountryResponse response = countryService.getCountryById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves a country by 2-letter ISO code (e.g. US, FR, JP).
     *
     * @param isoCode 2-letter ISO country code
     * @return Country details
     */
    @GetMapping("/code/{isoCode}")
    @Operation(summary = "Get country by ISO Code", description = "Retrieves country details by ISO-2 code (e.g. US, FR, AE).")
    public ResponseEntity<ApiResponse<CountryResponse>> getCountryByIsoCode(
            @Parameter(description = "2-letter ISO Code (e.g., US, IN)")
            @PathVariable String isoCode) {
        CountryResponse response = countryService.getCountryByIsoCode(isoCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
