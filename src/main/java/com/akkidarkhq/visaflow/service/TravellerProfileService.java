package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.dto.request.TravellerProfileRequest;
import com.akkidarkhq.visaflow.dto.response.TravellerResponseDto;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.TravellerProfile;
import com.akkidarkhq.visaflow.model.VisaApplication;
import com.akkidarkhq.visaflow.repository.TravellerProfileRepository;
import com.akkidarkhq.visaflow.repository.VisaApplicationRepository;
import com.akkidarkhq.visaflow.security.model.User;
import com.akkidarkhq.visaflow.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TravellerProfileService {

    private final TravellerProfileRepository travellerProfileRepository;
    private final UserRepository userRepository;
    private final VisaApplicationRepository visaApplicationRepository;

    @Transactional
    public TravellerResponseDto createTraveller(TravellerProfileRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (travellerProfileRepository.existsByUserIdAndPassportNumber(request.getUserId(), request.getPassportNumber())) {
            throw new IllegalArgumentException("A traveller profile with passport number " +
                    request.getPassportNumber() + " already exists for this user.");
        }

        TravellerProfile profile = TravellerProfile.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passportNumber(request.getPassportNumber())
                .passportExpiry(request.getPassportExpiry())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality() != null ? request.getNationality() : "Indian")
                .gender(request.getGender())
                .build();

        TravellerProfile savedProfile = travellerProfileRepository.save(profile);
        return mapToResponse(savedProfile);
    }

    /**
     * Retrieves the traveller profile associated with a specific Visa Application.
     *
     * @param applicationId UUID of the VisaApplication
     * @return TravellerResponseDto containing traveller biodata and passport details
     */
    @Transactional(readOnly = true)
    public TravellerResponseDto getTravellerByApplicationId(UUID applicationId) {
        VisaApplication application = visaApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + applicationId));

        if (application.getTraveller() == null) {
            throw new ResourceNotFoundException("No traveller profile associated with application: " + applicationId);
        }

        return mapToResponse(application.getTraveller());
    }

    @Transactional(readOnly = true)
    public TravellerResponseDto getTravellerById(UUID id) {
        TravellerProfile profile = travellerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + id));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<TravellerResponseDto> getTravellersByUserId(UUID userId) {
        return travellerProfileRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TravellerResponseDto mapToResponse(TravellerProfile profile) {
        return TravellerResponseDto.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .passportNumber(profile.getPassportNumber())
                .passportExpiry(profile.getPassportExpiry())
                .dateOfBirth(profile.getDateOfBirth())
                .nationality(profile.getNationality())
                .gender(profile.getGender())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
