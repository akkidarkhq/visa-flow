package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.dto.request.VisaTypeRequest;
import com.akkidarkhq.visaflow.dto.response.VisaTypeResponse;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.Country;
import com.akkidarkhq.visaflow.model.VisaType;
import com.akkidarkhq.visaflow.repository.CountryRepository;
import com.akkidarkhq.visaflow.repository.VisaTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisaTypeService {

    private final VisaTypeRepository visaTypeRepository;
    private final CountryRepository countryRepository;

    @Transactional
    public VisaTypeResponse createVisaType(VisaTypeRequest request) {
        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + request.getCountryId()));

        if (visaTypeRepository.existsByCountryIdAndName(request.getCountryId(), request.getName())) {
            throw new IllegalArgumentException("VisaType with name '" + request.getName() + "' already exists for country " + country.getName());
        }

        VisaType visaType = mapToEntity(request, country);
        VisaType saved = visaTypeRepository.save(visaType);
        return mapToResponse(saved);
    }

    /**
     * Bulk creation of multiple visa types in a single request.
     */
    @Transactional
    public List<VisaTypeResponse> createVisaTypesBulk(List<VisaTypeRequest> requests) {
        List<VisaTypeResponse> responses = new ArrayList<>();
        for (VisaTypeRequest request : requests) {
            responses.add(createVisaType(request));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<VisaTypeResponse> getAllActiveVisaTypes() {
        return visaTypeRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VisaTypeResponse> getVisaTypesByCountry(Long countryId) {
        if (!countryRepository.existsById(countryId)) {
            throw new ResourceNotFoundException("Country not found with id: " + countryId);
        }
        return visaTypeRepository.findByCountryIdAndIsActiveTrue(countryId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VisaTypeResponse getVisaTypeById(Long id) {
        VisaType visaType = visaTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisaType not found with id: " + id));
        return mapToResponse(visaType);
    }

    private VisaType mapToEntity(VisaTypeRequest request, Country country) {
        return VisaType.builder()
                .country(country)
                .name(request.getName())
                .processingTimeDays(request.getProcessingTimeDays())
                .serviceFee(request.getServiceFee())
                .embassyFee(request.getEmbassyFee())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }

    private VisaTypeResponse mapToResponse(VisaType visaType) {
        BigDecimal serviceFee = visaType.getServiceFee() != null ? visaType.getServiceFee() : BigDecimal.ZERO;
        BigDecimal embassyFee = visaType.getEmbassyFee() != null ? visaType.getEmbassyFee() : BigDecimal.ZERO;
        BigDecimal totalFee = serviceFee.add(embassyFee);

        return VisaTypeResponse.builder()
                .id(visaType.getId())
                .countryId(visaType.getCountry() != null ? visaType.getCountry().getId() : null)
                .countryName(visaType.getCountry() != null ? visaType.getCountry().getName() : null)
                .countryIsoCode(visaType.getCountry() != null ? visaType.getCountry().getIsoCode() : null)
                .name(visaType.getName())
                .processingTimeDays(visaType.getProcessingTimeDays())
                .serviceFee(serviceFee)
                .embassyFee(embassyFee)
                .totalFee(totalFee)
                .currency(visaType.getCurrency())
                .description(visaType.getDescription())
                .isActive(visaType.getIsActive())
                .createdAt(visaType.getCreatedAt())
                .build();
    }
}
