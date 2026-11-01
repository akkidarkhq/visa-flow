package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.ChecklistRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistRequirementRepository extends JpaRepository<ChecklistRequirement, Long> {
    List<ChecklistRequirement> findByVisaTypeIdOrderByDisplayOrderAsc(Long visaTypeId);
    List<ChecklistRequirement> findByVisaTypeIdAndIsMandatoryTrue(Long visaTypeId);
}
