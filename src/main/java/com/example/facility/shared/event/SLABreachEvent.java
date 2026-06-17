package com.example.facility.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a ticket SLA is breached
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SLABreachEvent {
    private Long ticketId;
    private String ticketNumber;
    private String breachType; // ACK, RESOLVE, or CLOSURE
    private String severityLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime breachedAt;
}

