package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.VisaOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisaOrderRepository extends JpaRepository<VisaOrder, UUID> {
    Optional<VisaOrder> findByOrderReference(String orderReference);
    boolean existsByOrderReference(String orderReference);
    List<VisaOrder> findByUserId(UUID userId);
}
