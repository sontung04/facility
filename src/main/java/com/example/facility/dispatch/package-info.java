/**
 * Dispatch module — technician skill management, ticket assignment, and performance tracking.
 *
 * <p>Public API exposed to other modules:
 * <ul>
 *   <li>{@link com.example.facility.dispatch.DispatchApi}</li>
 * </ul>
 *
 * <p>Note: {@code TechnicianSkill} retains a {@code @ManyToOne Category} JPA reference — this is a
 * known structural coupling at the persistence layer. Service-level dependencies on the
 * facility module go through {@link com.example.facility.facility.FacilityApi} only.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Dispatch",
        allowedDependencies = { "ticket", "facility", "facility::model", "notification", "shared", "identity" }
)
package com.example.facility.dispatch;
