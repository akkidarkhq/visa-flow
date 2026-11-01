package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.ApplicationDocument;
import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, UUID> {
    List<ApplicationDocument> findByApplicationId(UUID applicationId);
    List<ApplicationDocument> findByApplicationIdAndStatus(UUID applicationId, DocumentStatus status);
    Optional<ApplicationDocument> findByApplicationIdAndRequirementId(UUID applicationId, Long requirementId);
    boolean existsByApplicationIdAndRequirementId(UUID applicationId, Long requirementId);
}
