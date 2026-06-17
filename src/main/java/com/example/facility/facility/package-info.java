/**
 * Facility module — buildings, rooms, devices, and categories.
 *
 * <p>Public API exposed to other modules:
 * <ul>
 *   <li>{@link com.example.facility.facility.FacilityApi}</li>
 *   <li>{@link com.example.facility.facility.DeviceInfo}</li>
 *   <li>{@link com.example.facility.facility.CategoryInfo}</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Facility",
        allowedDependencies = { "shared", "identity" }
)
package com.example.facility.facility;
