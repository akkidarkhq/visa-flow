package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.VisaApplication;
import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Database-optimized query to fetch ONLY solo / individual applications (group size = 1).
     * Excludes any applications that belong to a group with 2 or more travellers.
     */
    @Query("SELECT a FROM VisaApplication a " +
           "WHERE (:userId IS NULL OR a.traveller.user.id = :userId) " +
           "AND a.group.id IN (" +
           "    SELECT g.id FROM ApplicationGroup g " +
           "    JOIN g.applications apps " +
           "    GROUP BY g.id " +
           "    HAVING COUNT(apps.id) = 1" +
           ") " +
           "ORDER BY a.createdAt DESC")
    List<VisaApplication> findSoloApplications(@Param("userId") UUID userId);
}
