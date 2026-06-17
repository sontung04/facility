package com.example.facility.dispatch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispatch_history", indexes = {
    @Index(name = "idx_dispatch_ticket_id", columnList = "ticket_id"),
    @Index(name = "idx_dispatch_technician_id", columnList = "technician_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class DispatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    
    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "previous_technician_id")
    private Long previousTechnicianId;

    @Column(name = "dispatcher_id", nullable = false)
    private Long dispatcherId;
    
    @Column(name = "dispatch_type", nullable = false)
    private String dispatchType; // MANUAL, AUTO
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}



