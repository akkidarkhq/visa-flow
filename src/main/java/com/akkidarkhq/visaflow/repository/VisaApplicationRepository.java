package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.VisaApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VisaApplicationRepository extends JpaRepository<VisaApplication, UUID> {
}
