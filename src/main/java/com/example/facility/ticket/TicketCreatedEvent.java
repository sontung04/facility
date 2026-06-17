package com.example.facility.ticket;

import java.time.LocalDateTime;

/**
 * Spring application event published by TicketService when a new ticket is created.
 * Consumed by SLAService to initialize SLA breach records.
 */
public record TicketCreatedEvent(
        Long ticketId,
        String ticketNumber,
        Long categoryId,
        Long reportedBy,
        LocalDateTime submittedAt
) {}
