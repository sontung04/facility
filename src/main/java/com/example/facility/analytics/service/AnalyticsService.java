package com.example.facility.analytics.service;

import com.example.facility.analytics.dto.response.AnalyticsResponse;
import com.example.facility.analytics.dto.response.MttrBreakdownResponse;
import com.example.facility.analytics.dto.response.SLABreachDetailResponse;
import com.example.facility.analytics.dto.response.TicketVolumeResponse;
import com.example.facility.dispatch.DispatchApi;
import com.example.facility.sla.SLABreachSummary;
import com.example.facility.sla.SlaApi;
import com.example.facility.ticket.TicketApi;
import com.example.facility.ticket.TicketSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final TicketApi ticketApi;
    private final DispatchApi dispatchApi;
    private final SlaApi slaApi;

    // ── FR-RPT-04: Filtered dashboard ────────────────────────────────────────

    @Transactional(readOnly = true)
    public AnalyticsResponse generateAnalytics(Long categoryId, Long buildingId,
            String severity, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            log.info("Generating analytics: categoryId={}, buildingId={}, severity={}, start={}, end={}",
                    categoryId, buildingId, severity, startDate, endDate);

            List<TicketSummary> tickets =
                    ticketApi.findForAnalytics(categoryId, buildingId, severity, startDate, endDate);

            long totalTickets    = tickets.size();
            long resolvedTickets = tickets.stream()
                    .filter(t -> "RESOLVED".equals(t.status()) || "CLOSED".equals(t.status()))
                    .count();
            long pendingTickets  = totalTickets - resolvedTickets;

            double averageMTTR = tickets.stream()
                    .filter(t -> t.resolvedAt() != null && t.submittedAt() != null)
                    .mapToLong(t -> ChronoUnit.MINUTES.between(t.submittedAt(), t.resolvedAt()))
                    .average()
                    .orElse(0.0);

            // countBreachedTicketsInRange counts *distinct tickets* with ≥1 breach,
            // preventing breach rate > 100% (a ticket can produce up to 3 breach records).
            long slaBreachCount = slaApi.countBreachedTicketsInRange(startDate, endDate);
            double slaBreachRate = totalTickets > 0
                    ? (slaBreachCount * 100.0 / totalTickets) : 0.0;

            long totalTechnicians = dispatchApi.getTotalTechnicianCount();
            double averageAssignmentLoad = calculateAverageAssignmentLoad(totalTechnicians);

            return AnalyticsResponse.builder()
                    .totalTickets(totalTickets)
                    .resolvedTickets(resolvedTickets)
                    .pendingTickets(pendingTickets)
                    .averageMTTR(averageMTTR)
                    .slaBreachRate(slaBreachRate)
                    .slaBreachCount(slaBreachCount)
                    .totalTechnicians(totalTechnicians)
                    .averageAssignmentLoad(averageAssignmentLoad)
                    .calculatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error generating analytics", e);
            return AnalyticsResponse.builder().calculatedAt(LocalDateTime.now()).build();
        }
    }

    // ── FR-RPT-01: Ticket volume ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TicketVolumeResponse> getTicketVolumeByPeriod(
            String groupBy, LocalDateTime startDate, LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime end   = endDate   != null ? endDate   : LocalDateTime.now();

        List<Object[]> rows = switch (groupBy == null ? "DAY" : groupBy.toUpperCase()) {
            case "WEEK"  -> ticketApi.countVolumeByWeek(start, end);
            case "MONTH" -> ticketApi.countVolumeByMonth(start, end);
            default      -> ticketApi.countVolumeByDay(start, end);
        };

        return rows.stream()
                .map(r -> new TicketVolumeResponse((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    // ── FR-RPT-02: MTTR breakdown ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MttrBreakdownResponse> getMttrBreakdown(
            String groupBy, LocalDateTime startDate, LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end   = endDate   != null ? endDate   : LocalDateTime.now();

        List<Object[]> rows = switch (groupBy == null ? "CATEGORY" : groupBy.toUpperCase()) {
            case "TECHNICIAN" -> ticketApi.getMttrByTechnician(start, end);
            case "BUILDING"   -> ticketApi.getMttrByBuilding(start, end);
            default           -> ticketApi.getMttrByCategory(start, end);
        };

        return rows.stream()
                .map(r -> MttrBreakdownResponse.builder()
                        .groupId(((Number) r[0]).longValue())
                        .groupName((String) r[1])
                        .avgMttrMinutes(r[2] != null ? ((Number) r[2]).doubleValue() : 0.0)
                        .ticketCount(((Number) r[3]).longValue())
                        .build())
                .toList();
    }

    // ── FR-RPT-03: SLA breach details ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SLABreachDetailResponse> getSLABreachDetails(
            LocalDateTime startDate, LocalDateTime endDate) {

        return slaApi.findBreachedWithDetails(startDate, endDate).stream()
                .map(sb -> SLABreachDetailResponse.builder()
                        .breachId(sb.breachId())
                        .ticketId(sb.ticketId())
                        .ticketNumber(sb.ticketNumber())
                        .breachType(sb.breachType())
                        .expectedBy(sb.expectedBy())
                        .actualBreachAt(sb.actualBreachAt())
                        .categoryName(sb.categoryName())
                        .deviceCode(sb.deviceCode())
                        .buildingName(sb.buildingName())
                        .severityLevel(sb.severityLevel())
                        .build())
                .toList();
    }

    // ── SLA compliance rate ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Double calculateSLAComplianceRate(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            long ticketCount = ticketApi.countBySubmittedAtBetween(startDate, endDate);
            if (ticketCount == 0) return 100.0;
            // Use distinct-ticket breach count so compliance rate cannot fall below 0%.
            long breachCount = slaApi.countBreachedTicketsInRange(startDate, endDate);
            return 100.0 - (breachCount * 100.0 / ticketCount);
        } catch (Exception e) {
            log.error("Error calculating SLA compliance rate", e);
            return 0.0;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double calculateAverageAssignmentLoad(long totalTechnicians) {
        if (totalTechnicians == 0) return 0.0;
        long activeCount = ticketApi.countByStatusIn(List.of("ASSIGNED", "IN_PROGRESS"));
        return (double) activeCount / totalTechnicians;
    }
}
