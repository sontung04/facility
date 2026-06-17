package com.example.facility.ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Public API of the Ticket module.
 * Other modules (Dispatch, Analytics) must go through this interface —
 * never inject TicketRepository or ResolutionFeedbackRepository directly.
 */
public interface TicketApi {

    // ── Dispatch ─────────────────────────────────────────────────────────────

    /** Fetch a single ticket summary, or throw RESOURCE_NOT_FOUND. */
    TicketSummary getTicketById(Long ticketId);

    /**
     * Assigns a ticket to a technician: updates status → ASSIGNED,
     * sets assignedTechnicianId / assignmentNotes / assignedAt, and persists.
     * Returns the updated summary.
     */
    TicketSummary performAssignment(Long ticketId, Long technicianId, String notes);

    /**
     * Returns a map of technicianId → active (non-closed) ticket count,
     * only including entries for technicians that have at least one active ticket.
     */
    Map<Long, Long> countActiveTicketsByTechnicianIds(List<Long> technicianIds);

    // ── TechnicianPerformanceService ─────────────────────────────────────────

    /** Find all tickets assigned to a technician whose status is in the given set (as String names). */
    List<TicketSummary> findByTechnicianIdAndStatusIn(Long technicianId, List<String> statuses);

    /**
     * Batch performance stats: [technicianId, resolvedCount, slaBreachCount].
     * Only includes technicians with at least one closed ticket.
     */
    List<Object[]> countPerformanceMetricsByTechnicianIds(List<Long> technicianIds);

    /** Returns [technicianId, avgRating] for quality scoring. */
    List<Object[]> avgRatingByTechnicianIds(List<Long> technicianIds);

    /** Returns the average resolution feedback rating for a single technician. */
    Double avgRatingByTechnicianId(Long technicianId);

    // ── Analytics ────────────────────────────────────────────────────────────

    List<TicketSummary> findForAnalytics(Long categoryId, Long buildingId,
            String severity, LocalDateTime startDate, LocalDateTime endDate);

    List<Object[]> countVolumeByDay(LocalDateTime startDate, LocalDateTime endDate);
    List<Object[]> countVolumeByWeek(LocalDateTime startDate, LocalDateTime endDate);
    List<Object[]> countVolumeByMonth(LocalDateTime startDate, LocalDateTime endDate);

    List<Object[]> getMttrByCategory(LocalDateTime startDate, LocalDateTime endDate);
    List<Object[]> getMttrByTechnician(LocalDateTime startDate, LocalDateTime endDate);
    List<Object[]> getMttrByBuilding(LocalDateTime startDate, LocalDateTime endDate);

    /** Count tickets submitted in the given date range (used for SLA compliance calculation). */
    long countBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /** Count tickets whose status is in the given set (as String names). */
    long countByStatusIn(List<String> statuses);
}
