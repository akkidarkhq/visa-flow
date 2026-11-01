package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.VisaApplication;
import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisaApplicationRepository extends JpaRepository<VisaApplication, UUID> {
    Optional<VisaApplication> findByReferenceNumber(String referenceNumber);
    boolean existsByReferenceNumber(String referenceNumber);
    Optional<VisaApplication> findByOrderItemId(UUID orderItemId);
    List<VisaApplication> findByOrderItemIdIn(List<UUID> orderItemIds);
    List<VisaApplication> findByTravellerId(UUID travellerId);
    boolean existsByOrderItemId(UUID orderItemId);
    List<VisaApplication> findByStatus(ApplicationStatus status);
    List<VisaApplication> findByTravellerUserId(UUID userId);
    List<VisaApplication> findByStatusAndTravellerUserId(ApplicationStatus status, UUID userId);
    List<VisaApplication> findByGroupReference(String groupReference);
    List<VisaApplication> findByGroupId(UUID groupId);
    Optional<VisaApplication> findByGroupReferenceAndTravellerId(String groupReference, UUID travellerId);
    boolean existsByGroupReferenceAndTravellerId(String groupReference, UUID travellerId);
}
