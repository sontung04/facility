/**
 * Audit module — immutable audit log for security-sensitive operations.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Audit",
        allowedDependencies = { "shared" }
)
package com.example.facility.audit;
