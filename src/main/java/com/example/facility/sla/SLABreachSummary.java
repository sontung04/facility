package com.example.facility.sla;

import java.time.LocalDateTime;

/**
 * Flat read-model of an SLA breach, pre-joined with ticket/device/building data.
 * Used by the Analytics module so it does not need to import SLABreach or Ticket entities.
 */
public record SLABreachSummary(
        Long breachId,
        String breachType,
        LocalDateTime expectedBy,
        LocalDateTime actualBreachAt,
        Long ticketId,
        String ticketNumber,
        String categoryName,
        String deviceCode,
        String buildingName,
        String severityLevel
) {}
