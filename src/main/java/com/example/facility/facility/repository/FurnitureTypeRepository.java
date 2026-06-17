package com.example.facility.facility.repository;

import com.example.facility.facility.model.FurnitureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FurnitureTypeRepository extends JpaRepository<FurnitureType, Long> {
    boolean existsByTypeCode(String typeCode);
    Optional<FurnitureType> findByTypeCode(String typeCode);
}
