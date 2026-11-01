package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.dto.request.CountryRequest;
import com.akkidarkhq.visaflow.dto.response.CountryResponse;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.Country;
import com.akkidarkhq.visaflow.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    @Transactional
    public CountryResponse createCountry(CountryRequest request) {
        String isoCodeUpper = request.getIsoCode().trim().toUpperCase();
        String nameTrimmed = request.getName().trim();

        if (countryRepository.existsByIsoCodeIgnoreCase(isoCodeUpper)) {
            throw new IllegalArgumentException("Country with ISO code '" + isoCodeUpper + "' already exists");
        }

        if (countryRepository.existsByNameIgnoreCase(nameTrimmed)) {
            throw new IllegalArgumentException("Country with name '" + nameTrimmed + "' already exists");
        }

        Country country = Country.builder()
                .isoCode(isoCodeUpper)
                .name(nameTrimmed)
                .flagIconUrl(request.getFlagIconUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Country saved = countryRepository.save(country);
        return mapToResponse(saved);
    }

    /**
     * Transactional bulk creation.
     * Rolls back completely if any entry fails validation or duplicate check.
     */
    @Transactional
    public List<CountryResponse> createCountries(List<CountryRequest> requests) {
        List<CountryResponse> responses = new ArrayList<>();
        for (CountryRequest request : requests) {
            responses.add(createCountry(request));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> getAllActiveCountries() {
        return countryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CountryResponse getCountryById(Long id) {
        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));
        return mapToResponse(country);
    }

    @Transactional(readOnly = true)
    public CountryResponse getCountryByIsoCode(String isoCode) {
        Country country = countryRepository.findByIsoCodeIgnoreCase(isoCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with ISO code: " + isoCode));
        return mapToResponse(country);
    }

    private CountryResponse mapToResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .isoCode(country.getIsoCode())
                .name(country.getName())
                .flagIconUrl(country.getFlagIconUrl())
                .isActive(country.getIsActive())
                .createdAt(country.getCreatedAt())
                .build();
    }
}
