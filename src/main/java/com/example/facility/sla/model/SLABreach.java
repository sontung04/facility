package com.example.facility.sla.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.facility.ticket.model.Ticket;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_breaches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SLABreach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "breach_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private BreachType breachType; // ACK_BREACH, RESOLVE_BREACH, CLOSURE_BREACH

    @Column(name = "expected_by", nullable = false)
    private LocalDateTime expectedBy;

    @Column(name = "actual_breach_at")
    private LocalDateTime actualBreachAt;

    @Builder.Default
    @Column(name = "is_breached", nullable = false)
    private Boolean isBreached = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum BreachType {
        ACK_BREACH, RESOLVE_BREACH, CLOSURE_BREACH
    }
}

