package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.ApplicationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationGroupRepository extends JpaRepository<ApplicationGroup, UUID> {

    Optional<ApplicationGroup> findByGroupReference(String groupReference);

    boolean existsByGroupReference(String groupReference);

    List<ApplicationGroup> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ApplicationGroup> findAllByOrderByCreatedAtDesc();

    @Query("SELECT g FROM ApplicationGroup g " +
           "JOIN FETCH g.visaType vt " +
           "LEFT JOIN FETCH vt.country " +
           "WHERE g.groupReference = :groupReference")
    Optional<ApplicationGroup> findByGroupReferenceWithDetails(@Param("groupReference") String groupReference);
}
