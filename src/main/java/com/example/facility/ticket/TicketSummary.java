package com.example.facility.ticket;

import java.time.LocalDateTime;

/**
 * Public read-model of a Ticket for use by other modules (Dispatch, Analytics).
 * All status / severity fields are plain Strings to avoid exposing internal enums.
 */
public record TicketSummary(
        Long id,
        String ticketNumber,
        String status,
        String severityLevel,
        Float severityScore,
        String description,
        Long categoryId,
        String categoryName,
        Long deviceId,
        String deviceCode,
        Long reportedBy,
        Long assignedTechnicianId,
        String assignmentNotes,
        LocalDateTime submittedAt,
        LocalDateTime ackAt,
        LocalDateTime assignedAt,
        LocalDateTime inProgressAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        Boolean slaAckBreached,
        Boolean slaResolveBreached,
        Boolean slaClosureBreached
) {}
