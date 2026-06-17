package com.example.facility.dispatch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibleTechnicianResponse {

    private Long    technicianId;
    private String  username;

    private Long    categoryId;
    private String  categoryName;

    /** BEGINNER | INTERMEDIATE | EXPERT */
    private String  skillLevel;

    private Integer maxConcurrentJobs;

    /** Number of active (non-closed) tickets currently assigned. */
    private long    activeTickets;

    private boolean available;

    /** true when activeTickets >= maxConcurrentJobs */
    private boolean atCapacity;

    // ── Dispatch score breakdown ─────────────────────────────────────────────
    // Populated by TechnicianSkillService.getEligibleTechnicians().
    // All component scores are in [0.0, 1.0]; lower = better candidate.

    /**
     * Rank within the eligible pool for this ticket (1 = best candidate).
     * Only set when a ticketId is provided.
     */
    private Integer rank;

    /**
     * Weighted composite score (lower = better).
     * = w_load × workloadScore + w_skill × skillScore
     *   + w_quality × qualityScore + w_fairness × fairnessScore
     */
    private Double compositeScore;

    /**
     * Workload component: activeTickets / maxConcurrentJobs.
     * 0.0 = free, 1.0 = at capacity.
     */
    private Double workloadScore;

    /**
     * Skill mismatch component: how far below the required skill level this technician is.
     * 0.0 = meets or exceeds requirement, 0.5 = one level below, 1.0 = two levels below.
     */
    private Double skillScore;

    /**
     * Quality penalty component: 1.0 − historical quality score.
     * 0.0 = excellent track record, 1.0 = poor track record.
     */
    private Double qualityScore;

    /**
     * Fairness penalty component: how recently this technician was last assigned.
     * 0.0 = not assigned recently (most fair), 1.0 = just assigned (least fair).
     */
    private Double fairnessScore;

    /**
     * Urgency at scoring time: ratio of ticket waiting time to SLA resolution window.
     * 0.0 = just submitted, 1.0 = at/past SLA deadline.
     * Drives the dynamic weight shift (higher urgency → more weight on skill+quality).
     */
    private Double urgency;
}
