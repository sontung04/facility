package com.example.facility.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a ticket is assigned to a technician
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssignedEvent {
    private Long ticketId;
    private String ticketNumber;
    private Long technicianId;
    private Long dispatcherId;
    private String dispatchType; // MANUAL or AUTO
    private String notes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime assignedAt;
}

