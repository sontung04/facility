package com.example.facility.sla;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public API of the SLA module.
 * Used by the Analytics module to query SLA breach data.
 */
public interface SlaApi {

    /**
     * Count breached SLA *records* (up to 3 per ticket: ACK, RESOLVE, CLOSURE) in the given range.
     * Pass {@code null} for either bound to skip that constraint.
     */
    long countBreachedInRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count *distinct tickets* that have at least one breach in the given range.
     * Use this for breach-rate and compliance-rate calculations to prevent rates > 100%.
     * Pass {@code null} for either bound to skip that constraint.
     */
    long countBreachedTicketsInRange(LocalDateTime startDate, LocalDateTime endDate);

    /** Return a flat list of SLA breaches with all related ticket/device/building data pre-joined. */
    List<SLABreachSummary> findBreachedWithDetails(LocalDateTime startDate, LocalDateTime endDate);
}
