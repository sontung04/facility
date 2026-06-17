package com.example.facility.dispatch;

/**
 * Public API of the Dispatch module.
 * Used by Analytics to retrieve technician count without importing dispatch internals.
 */
public interface DispatchApi {

    /** Total number of technician skill records (used as a proxy for total active technicians). */
    long getTotalTechnicianCount();
}
