package com.example.facility.dispatch.service;

import com.example.facility.dispatch.dto.response.TechnicianPerformanceResponse;
import com.example.facility.ticket.TicketApi;
import com.example.facility.ticket.TicketSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechnicianPerformanceService {

    private final TicketApi ticketApi;

    private static final List<String> CLOSED_STATUSES  = List.of("RESOLVED", "CLOSED");
    private static final List<String> ACTIVE_STATUSES  =
            List.of("SUBMITTED", "ACK", "ASSIGNED", "IN_PROGRESS");

    /**
     * Batch quality scores for auto-assign scoring. Returns [0.0, 1.0] where 1.0 = best.
     * Technicians with no history default to 0.5 (neutral).
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> computeQualityScores(List<Long> technicianIds) {
        List<Object[]> ticketStats = ticketApi.countPerformanceMetricsByTechnicianIds(technicianIds);
        List<Object[]> ratingStats = ticketApi.avgRatingByTechnicianIds(technicianIds);

        Map<Long, Double> avgRatings = ratingStats.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> ((Number) r[1]).doubleValue()));

        Map<Long, Double> scores = new HashMap<>();
        for (Object[] row : ticketStats) {
            Long id     = (Long) row[0];
            long resolved = ((Number) row[1]).longValue();
            long breaches = ((Number) row[2]).longValue();
            double slaCompliance = resolved > 0 ? 1.0 - ((double) breaches / resolved) : 0.5;
            double rating = avgRatings.containsKey(id)
                    ? (avgRatings.get(id) - 1.0) / 4.0
                    : 0.5;
            scores.put(id, slaCompliance * 0.6 + rating * 0.4);
        }
        technicianIds.forEach(id -> scores.putIfAbsent(id, 0.5));
        return scores;
    }

    /**
     * Full performance view for a single technician (used by the API endpoint).
     */
    @Transactional(readOnly = true)
    public TechnicianPerformanceResponse getTechnicianPerformance(Long technicianId) {
        List<TicketSummary> closedTickets =
                ticketApi.findByTechnicianIdAndStatusIn(technicianId, CLOSED_STATUSES);
        List<TicketSummary> activeTickets =
                ticketApi.findByTechnicianIdAndStatusIn(technicianId, ACTIVE_STATUSES);

        long resolvedCount = closedTickets.size();

        double avgResolutionTimeMinutes = closedTickets.stream()
                .filter(t -> t.resolvedAt() != null && t.assignedAt() != null)
                .mapToLong(t -> ChronoUnit.MINUTES.between(t.assignedAt(), t.resolvedAt()))
                .average()
                .orElse(0.0);

        long breaches = closedTickets.stream()
                .filter(t -> Boolean.TRUE.equals(t.slaResolveBreached()))
                .count();
        double slaComplianceRate = resolvedCount > 0
                ? 1.0 - ((double) breaches / resolvedCount)
                : 1.0;

        Double avgRating = ticketApi.avgRatingByTechnicianId(technicianId);

        // Compute qualityScore inline from data already loaded above.
        // Previously this called computeQualityScores(List.of(id)) which fired two extra
        // aggregate queries (countPerformanceMetrics + avgRating) for data we already have.
        // Formula mirrors computeQualityScores: slaCompliance*0.6 + normalizedRating*0.4.
        // The slaCompliance used here defaults to 0.5 (neutral) when there are no resolved
        // tickets, matching the batch method's convention.
        double slaForQuality    = resolvedCount > 0 ? slaComplianceRate : 0.5;
        double normalizedRating = avgRating != null ? (avgRating - 1.0) / 4.0 : 0.5;
        double qualityScore     = slaForQuality * 0.6 + normalizedRating * 0.4;

        return TechnicianPerformanceResponse.builder()
                .technicianId(technicianId)
                .resolvedCount(resolvedCount)
                .avgResolutionTimeMinutes(avgResolutionTimeMinutes)
                .slaComplianceRate(slaComplianceRate)
                .avgRating(avgRating)
                .qualityScore(qualityScore)
                .activeTicketCount((long) activeTickets.size())
                .build();
    }
}
