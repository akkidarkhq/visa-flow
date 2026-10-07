package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.VisaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface VisaTypeRepository extends JpaRepository<VisaType, Long> {
}

