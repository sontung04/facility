package com.example.facility.ticket.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

import com.example.facility.facility.model.Category;
import com.example.facility.facility.model.Device;
import com.example.facility.sla.model.SLABreach;

@Data
@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_device_id", columnList = "device_id"),
        @Index(name = "idx_tickets_status", columnList = "status"),
        @Index(name = "idx_tickets_submitted_at", columnList = "submitted_at")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.SUBMITTED;

    @Column(name = "severity_score", nullable = false)
    private Float severityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityLevel severityLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "assigned_technician_id")
    private Long assignedTechnicianId;

    @Column(name = "assignment_notes", columnDefinition = "TEXT")
    private String assignmentNotes;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "ack_at")
    private LocalDateTime ackAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "in_progress_at")
    private LocalDateTime inProgressAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Builder.Default
    @Column(name = "sla_ack_breached")
    private Boolean slaAckBreached = false;

    @Builder.Default
    @Column(name = "sla_resolve_breached")
    private Boolean slaResolveBreached = false;

    @Builder.Default
    @Column(name = "sla_closure_breached")
    private Boolean slaClosureBreached = false;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SLABreach> slaBreaches;

    @Column(name = "reported_by")
    private Long reportedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = submittedAt != null ? submittedAt : LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

