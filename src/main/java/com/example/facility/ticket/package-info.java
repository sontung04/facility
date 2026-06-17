/**
 * Ticket module — manages the full lifecycle of facility repair tickets.
 *
 * <p>Public API exposed to other modules:
 * <ul>
 *   <li>{@link com.example.facility.ticket.TicketApi} — read/write operations for external callers</li>
 *   <li>{@link com.example.facility.ticket.TicketSummary} — lightweight ticket projection</li>
 *   <li>{@link com.example.facility.ticket.TicketCreatedEvent} — published when a ticket is created</li>
 * </ul>
 *
 * <p>Internal sub-packages ({@code model}, {@code repository}, {@code service},
 * {@code controller}, {@code dto}, {@code consumer}) must not be imported by other modules,
 * except via the {@code ticket::model} named interface for JPA entity access.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Ticket",
        allowedDependencies = { "facility", "facility::model", "sla::model", "shared", "notification" }
)
package com.example.facility.ticket;
