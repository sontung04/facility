package com.example.facility.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a ticket status changes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusChangedEvent {
    private Long ticketId;
    private String ticketNumber;
    private String oldStatus;
    private String newStatus;
    private Long changedByUserId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime changedAt;
}

