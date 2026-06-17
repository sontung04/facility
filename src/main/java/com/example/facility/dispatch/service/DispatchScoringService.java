package com.example.facility.dispatch.service;

import com.example.facility.dispatch.model.TechnicianSkill;
import com.example.facility.dispatch.repository.DispatchHistoryRepository;
import com.example.facility.ticket.TicketSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>Adaptive Weighted Composite Dispatch Scoring</h2>
 *
 * <p>Scores a candidate technician against a specific ticket.  <b>Lower score = better</b>.
 *
 * <h3>The four factors</h3>
 * <ol>
 *   <li><b>Workload</b> — {@code activeTickets / maxConcurrentJobs}.<br>
 *       Prevents overloading any one technician.  Already in every dispatch system.</li>
 *
 *   <li><b>Skill match</b> — deficit between the minimum required skill ordinal for the
 *       ticket's severity and the technician's actual ordinal (BEGINNER=0, INTERMEDIATE=1,
 *       EXPERT=2).<br>
 *       Improvement over the old binary 0/0.3/1.0 penalty:
 *       the ordinal difference gives a proportional penalty, and
 *       the raw {@code severityScore} (1.0–5.0) is used to detect HIGH tickets close to the
 *       CRITICAL boundary (≥ 4.2) and upgrade their required skill to EXPERT.</li>
 *
 *   <li><b>Quality</b> — {@code 1 - qualityScore} where quality is a weighted composite of
 *       historical SLA compliance (60 %) and normalised feedback rating (40 %).<br>
 *       Rewards technicians who consistently resolve tickets on time with good ratings.</li>
 *
 *   <li><b>Fairness</b> — recency of the last dispatch.  A technician just assigned gets the
 *       maximum penalty (1.0); one who hasn't been assigned in more than
 *       {@value #FAIRNESS_CAP_MINUTES} minutes gets 0.0.<br>
 *       Prevents the same "best" technician from receiving every ticket when the top
 *       candidates are otherwise identical.</li>
 * </ol>
 *
 * <h3>Adaptive weight blending (the key graduation-project insight)</h3>
 * <p>The four weights shift linearly as the ticket ages toward its SLA resolution deadline
 * (urgency ∈ [0, 1]).  When the ticket is fresh, workload and fairness dominate (spread
 * the work evenly).  As the deadline approaches, skill and quality take over (send the
 * best person, not just the least-loaded one).
 *
 * <pre>
 *                      urgency = 0    urgency = 1
 *   w_workload          0.35    →      0.20
 *   w_skill             0.25    →      0.45
 *   w_quality           0.20    →      0.30
 *   w_fairness          0.20    →      0.05
 *   ─────────────────────────────────────────
 *   Sum                 1.00           1.00
 * </pre>
 *
 * <h3>Formula</h3>
 * <pre>
 *   score = w_workload  × workloadRatio
 *         + w_skill     × skillMismatch
 *         + w_quality   × qualityPenalty
 *         + w_fairness  × fairnessPenalty
 * </pre>
 *
 * <p>All components are normalised to [0.0, 1.0].  The composite is therefore also in
 * [0.0, 1.0].  Pick the candidate with the minimum composite score.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchScoringService {

    private final DispatchHistoryRepository dispatchHistoryRepository;

    // ── Constants ─────────────────────────────────────────────────────────────

    /**
     * Conservative SLA resolution windows per severity (minutes).  The actual policy lives
     * in the {@code sla_policies} table (sla module), which the dispatch module must not
     * access directly.  These defaults are used only to normalise the urgency factor.
     */
    public static final Map<String, Integer> SLA_RESOLUTION_MINUTES = Map.of(
            "CRITICAL",  60,   //  1 hour
            "HIGH",     240,   //  4 hours
            "MEDIUM",   480,   //  8 hours
            "LOW",     1440    // 24 hours
    );

    /**
     * Technicians who have not received a ticket in this many minutes are treated as
     * fully "stale" for fairness purposes — their fairness penalty is 0.0.
     */
    static final int FAIRNESS_CAP_MINUTES = 480; // 8 hours

    /** Numeric skill level → ordinal mapping used for mismatch calculation. */
    private static final Map<String, Integer> SKILL_ORDINAL = Map.of(
            "BEGINNER",      0,
            "INTERMEDIATE",  1,
            "EXPERT",        2
    );

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Compute the dispatch score for one candidate against the given ticket.
     *
     * @param skill          the technician's skill record for the ticket's category
     * @param activeTickets  count of non-closed tickets currently assigned to this technician
     * @param qualityScore   historical quality [0.0, 1.0] (1.0 = best) from
     *                       {@link TechnicianPerformanceService#computeQualityScores}
     * @param lastAssignedAt time of the technician's most recent dispatch assignment,
     *                       or {@code null} if they have never been assigned
     * @param ticket         the ticket about to be assigned
     * @return a {@link ScoreBreakdown} containing the composite total and all components
     */
    public ScoreBreakdown score(TechnicianSkill skill,
                                long activeTickets,
                                double qualityScore,
                                LocalDateTime lastAssignedAt,
                                TicketSummary ticket) {

        String severity     = ticket.severityLevel() != null ? ticket.severityLevel() : "LOW";
        Float  severityNum  = ticket.severityScore();

        double workloadRatio = (double) activeTickets
                / Math.max(1, skill.getMaxConcurrentJobs());
        workloadRatio = Math.min(1.0, workloadRatio);

        // ── Factor 2: Skill mismatch ────────────────────────────────────────
        // requiredOrdinal rises with severity; HIGH tickets whose numeric score is ≥ 4.2
        // (close to the CRITICAL threshold of 4.5) are treated as CRITICAL for skill routing.
        int requiredOrdinal = requiredSkillOrdinal(severity, severityNum);
        int actualOrdinal   = SKILL_ORDINAL.getOrDefault(skill.getSkillLevel(), 0);
        // Normalised to [0, 1]: 0 = matches/exceeds, 0.5 = one level short, 1.0 = two levels short
        double skillMismatch = Math.max(0.0, requiredOrdinal - actualOrdinal) / 2.0;

        double qualityPenalty = 1.0 - Math.max(0.0, Math.min(1.0, qualityScore));

        // ── Factor 4: Fairness penalty ──────────────────────────────────────
        // 1.0 = just assigned (poor fairness), 0.0 = never assigned or long ago (good fairness)
        double fairnessPenalty = 0.0;
        if (lastAssignedAt != null) {
            long minutesSince = ChronoUnit.MINUTES.between(lastAssignedAt, LocalDateTime.now());
            fairnessPenalty = Math.max(0.0, 1.0 - (double) minutesSince / FAIRNESS_CAP_MINUTES);
        }

        // ── Urgency [0, 1] ──────────────────────────────────────────────────
        // Ratio of time the ticket has already waited to the full SLA resolution window.
        // Drives the adaptive weight shift.
        int  slaMinutes    = SLA_RESOLUTION_MINUTES.getOrDefault(severity, 480);
        long minutesWaiting = ticket.submittedAt() != null
                ? Math.max(0, ChronoUnit.MINUTES.between(ticket.submittedAt(), LocalDateTime.now()))
                : 0;
        double urgency = Math.min(1.0, (double) minutesWaiting / slaMinutes);

        // If the ACK SLA has already been breached, treat urgency as at least 0.5 to ensure
        // a skilled technician is selected even if the ticket was just submitted in wall-clock time.
        if (Boolean.TRUE.equals(ticket.slaAckBreached())) {
            urgency = Math.max(urgency, 0.5);
        }

        double wWorkload  = lerp(0.35, 0.20, urgency);
        double wSkill     = lerp(0.25, 0.45, urgency);
        double wQuality   = lerp(0.20, 0.30, urgency);
        double wFairness  = lerp(0.20, 0.05, urgency);

        // ── Composite ───────────────────────────────────────────────────────
        double total = wWorkload  * workloadRatio
                     + wSkill     * skillMismatch
                     + wQuality   * qualityPenalty
                     + wFairness  * fairnessPenalty;

        // SLF4J uses {} placeholders — printf-style format specs are not supported.
        log.trace("Score tech={} ticket={} urgency={} load={} skill={} quality={} fairness={} total={}",
                skill.getUserId(), ticket.id(),
                String.format("%.2f", urgency),
                String.format("%.2f", workloadRatio),
                String.format("%.2f", skillMismatch),
                String.format("%.2f", qualityPenalty),
                String.format("%.2f", fairnessPenalty),
                String.format("%.3f", total));

        return new ScoreBreakdown(
                total, workloadRatio, skillMismatch, qualityPenalty, fairnessPenalty, urgency);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Minimum skill ordinal required for the given severity and numeric score.
     * <ul>
     *   <li>CRITICAL always needs EXPERT (ordinal 2).</li>
     *   <li>HIGH normally needs INTERMEDIATE (1), but if the raw score ≥ 4.2 it is close
     *       enough to CRITICAL that EXPERT is preferred.</li>
     *   <li>MEDIUM and LOW accept any skill level (0).</li>
     * </ul>
     */
    private int requiredSkillOrdinal(String severity, Float severityScore) {
        return switch (severity) {
            case "CRITICAL" -> 2;
            case "HIGH"     -> (severityScore != null && severityScore >= 4.2f) ? 2 : 1;
            default         -> 0;
        };
    }

    /**
     * Batch-loads the most recent dispatch timestamp for each technician ID.
     * Centralised here so both {@link DispatchService} and {@link TechnicianSkillService}
     * can call the same implementation instead of duplicating it.
     *
     * <p>Technicians with no dispatch history are absent from the returned map;
     * callers treat a missing key as {@code null} → "never assigned" → fairness penalty = 0.
     */
    public Map<Long, LocalDateTime> loadLastAssignmentTimes(List<Long> ids) {
        return dispatchHistoryRepository
                .findLastAssignmentTimesByTechnicianIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long)          row[0],
                        row -> (LocalDateTime) row[1]
                ));
    }

    /** Linear interpolation between {@code a} (t=0) and {@code b} (t=1). */
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // ── Value type ─────────────────────────────────────────────────────────────

    /**
     * Immutable score breakdown for a single technician/ticket pair.
     * All component values are in [0.0, 1.0]; the composite {@code total} is
     * the adaptive-weighted sum and is also in [0.0, 1.0].
     * <b>Lower total = better candidate.</b>
     */
    public record ScoreBreakdown(
            /** Weighted composite — the ranking key.  Lower = better. */
            double total,
            /** {@code activeTickets / maxConcurrentJobs}.  0 = free, 1 = at capacity. */
            double workload,
            /** Skill deficit.  0 = meets/exceeds requirement, 1 = two levels below. */
            double skillMismatch,
            /** {@code 1 - qualityScore}.  0 = excellent history, 1 = poor history. */
            double qualityPenalty,
            /** Recency penalty.  0 = not recently assigned, 1 = just assigned. */
            double fairnessPenalty,
            /** Ticket urgency at scoring time.  0 = fresh, 1 = at/past SLA deadline. */
            double urgency
    ) {}
}
