package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByIsoCodeIgnoreCase(String isoCode);
    Optional<Country> findByNameIgnoreCase(String name);
    List<Country> findByIsActiveTrueOrderByNameAsc();
    boolean existsByIsoCodeIgnoreCase(String isoCode);
    boolean existsByNameIgnoreCase(String name);
}
