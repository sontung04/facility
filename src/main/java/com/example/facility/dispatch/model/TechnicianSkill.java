package com.example.facility.dispatch.model;

import com.example.facility.facility.model.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "technician_skills", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "category_id" }) })
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "skill_level", nullable = false)
    private String skillLevel; // BEGINNER, INTERMEDIATE, EXPERT

    @Column(name = "max_concurrent_jobs")
    private Integer maxConcurrentJobs = 5;

    @Column(name = "available", nullable = false)
    private boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}


