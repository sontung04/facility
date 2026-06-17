package com.example.facility.sla.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.facility.facility.model.Category;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SLAPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // SLA thresholds in minutes
    @Builder.Default
    @Column(name = "acknowledgment_time_minutes", nullable = false)
    private Integer acknowledgmentTimeMinutes = 30;

    @Builder.Default
    @Column(name = "resolution_time_minutes", nullable = false)
    private Integer resolutionTimeMinutes = 240;

    @Builder.Default
    @Column(name = "closure_time_minutes", nullable = false)
    private Integer closureTimeMinutes = 1440;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

