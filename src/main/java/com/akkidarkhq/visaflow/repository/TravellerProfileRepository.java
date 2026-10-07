package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.TravellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TravellerProfileRepository extends JpaRepository<TravellerProfile, UUID> {
}
