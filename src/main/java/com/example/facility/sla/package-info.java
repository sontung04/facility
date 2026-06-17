/**
 * SLA module — SLA policies, breach detection, and compliance tracking.
 *
 * <p>Public API exposed to other modules:
 * <ul>
 *   <li>{@link com.example.facility.sla.SlaApi} — breach counts + detail list for Analytics</li>
 *   <li>{@link com.example.facility.sla.SLABreachSummary} — flat read-model of a breach</li>
 * </ul>
 *
 * <p>Listens to {@link com.example.facility.ticket.TicketCreatedEvent} via
 * {@link org.springframework.modulith.events.ApplicationModuleListener} to initialize
 * SLA breach records after a ticket is committed.
 *
 * <p>Note: {@code SLABreach} retains a {@code @ManyToOne Ticket} JPA reference and
 * {@code SLAPolicy} retains a {@code @ManyToOne Category} JPA reference — these are known
 * structural couplings at the persistence layer, exposed via the {@code ticket::model} and
 * {@code facility::model} named interfaces. Service-level dependencies on the ticket module
 * go through {@link com.example.facility.ticket.TicketCreatedEvent} only;
 * {@code EntityManager.getReference} is used in place of direct repository injection.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "SLA",
        allowedDependencies = { "ticket", "ticket::model", "facility::model", "notification", "shared" }
)
package com.example.facility.sla;
