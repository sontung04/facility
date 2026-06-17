/**
 * Named interface "model" — exposes {@code SLABreach} for the bidirectional
 * {@code Ticket.@OneToMany List<SLABreach>} JPA reference in the ticket module.
 * Other modules must NOT call SLA service or repository types directly.
 */
@org.springframework.modulith.NamedInterface("model")
package com.example.facility.sla.model;
