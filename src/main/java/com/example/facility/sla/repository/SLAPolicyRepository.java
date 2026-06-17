package com.example.facility.sla.repository;

import com.example.facility.sla.model.SLAPolicy;
import com.example.facility.facility.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SLAPolicyRepository extends JpaRepository<SLAPolicy, Long> {
    Optional<SLAPolicy> findByCategoryAndIsActiveTrue(Category category);

    /** Lookup without loading the Category entity — used by the TicketCreatedEvent listener. */
    Optional<SLAPolicy> findByCategoryIdAndIsActiveTrue(Long categoryId);
}

