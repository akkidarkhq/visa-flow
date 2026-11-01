package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.VisaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisaTypeRepository extends JpaRepository<VisaType, Long> {
    List<VisaType> findByCountryIdAndIsActiveTrue(Long countryId);
    List<VisaType> findByIsActiveTrue();
    boolean existsByCountryIdAndName(Long countryId, String name);
}
