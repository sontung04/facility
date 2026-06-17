package com.example.facility.ticket.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resolution_feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = "ticket_id", name = "uk_feedback_ticket")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private Long ticketId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "rated_by", nullable = false)
    private Long ratedBy;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

