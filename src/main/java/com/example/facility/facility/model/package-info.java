/**
 * Named interface "model" — exposes JPA entities ({@code Category}, {@code Device},
 * {@code Room}, {@code Building}) for cross-module use at the persistence layer
 * (Ticket, SLAPolicy, TechnicianSkill all carry @ManyToOne FK references into this package).
 * Other modules must NOT call facility service or repository types directly.
 */
@org.springframework.modulith.NamedInterface("model")
package com.example.facility.facility.model;
