/**
 * Named interface "model" — exposes JPA entities ({@code Ticket}, {@code TicketStatus},
 * {@code SeverityLevel}) for cross-module use by the SLA module's persistence layer.
 * Other modules must NOT call ticket service or repository types directly.
 */
@org.springframework.modulith.NamedInterface("model")
package com.example.facility.ticket.model;
