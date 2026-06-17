/**
 * Analytics module — dashboard, ticket volume, MTTR breakdowns, and SLA compliance.
 *
 * <p>Reads ticket data through {@link com.example.facility.ticket.TicketApi} and
 * technician counts through {@link com.example.facility.dispatch.DispatchApi}.
 * No direct repository access to other modules.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Analytics",
        allowedDependencies = { "ticket", "dispatch", "sla", "shared" }
)
package com.example.facility.analytics;
