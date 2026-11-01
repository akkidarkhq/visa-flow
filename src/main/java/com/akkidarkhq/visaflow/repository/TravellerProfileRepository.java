package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.TravellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TravellerProfileRepository extends JpaRepository<TravellerProfile, UUID> {
    List<TravellerProfile> findByUserId(UUID userId);
    Optional<TravellerProfile> findByUserIdAndPassportNumber(UUID userId, String passportNumber);
    boolean existsByUserIdAndPassportNumber(UUID userId, String passportNumber);
}
