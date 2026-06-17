package com.example.facility.dispatch.repository;

import com.example.facility.dispatch.model.TechnicianSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianSkillRepository extends JpaRepository<TechnicianSkill, Long> {
    List<TechnicianSkill> findByUserId(Long userId);

    List<TechnicianSkill> findByCategoryId(Long categoryId);

    // L7: only available technicians for auto-assign
    List<TechnicianSkill> findByCategoryIdAndAvailableTrue(Long categoryId);

    Optional<TechnicianSkill> findByUserIdAndCategoryId(Long userId, Long categoryId);
}



