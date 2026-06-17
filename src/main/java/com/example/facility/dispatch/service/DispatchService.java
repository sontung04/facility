package com.example.facility.dispatch.service;

import com.example.facility.dispatch.dto.request.AssignTicketRequest;
import com.example.facility.dispatch.dto.response.DispatchHistoryResponse;
import com.example.facility.dispatch.model.DispatchHistory;
import com.example.facility.dispatch.model.TechnicianSkill;
import com.example.facility.dispatch.repository.DispatchHistoryRepository;
import com.example.facility.dispatch.repository.TechnicianSkillRepository;
import com.example.facility.notification.NotificationApi;
import com.example.facility.shared.event.TicketAssignedEvent;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.ticket.TicketApi;
import com.example.facility.ticket.TicketSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final TicketApi                  ticketApi;
    private final TechnicianSkillRepository  technicianSkillRepository;
    private final DispatchHistoryRepository  dispatchHistoryRepository;
    private final TechnicianPerformanceService performanceService;
    private final DispatchScoringService     scoringService;
    private final NotificationApi            notificationApi;
    private final ApplicationEventPublisher  eventPublisher;

    // ── Main assignment entry point ───────────────────────────────────────────

    @Transactional
    public TicketSummary assignTicket(AssignTicketRequest request, Long dispatcherId) {
        TicketSummary ticket = ticketApi.getTicketById(request.getTicketId());
        Long previousTechnicianId = ticket.assignedTechnicianId();

        Long technicianId;
        if (request.isAutoAssign()) {
            technicianId = autoSelectTechnician(ticket);
            log.info("Auto-selected technician {} for ticket {} (severity={}, severityScore={})",
                    technicianId, ticket.ticketNumber(), ticket.severityLevel(), ticket.severityScore());
        } else {
            technicianId = request.getTechnicianId();
            validateTechnicianForManualAssign(technicianId, ticket.categoryId(), request.isForceAssign());
        }

        TicketSummary updated = ticketApi.performAssignment(
                request.getTicketId(), technicianId, request.getNotes());

        String dispatchType = determineDispatchType(
                request.isAutoAssign(), previousTechnicianId, technicianId);

        DispatchHistory history = new DispatchHistory();
        history.setTicketId(updated.id());
        history.setTechnicianId(technicianId);
        history.setPreviousTechnicianId(previousTechnicianId);
        history.setDispatcherId(dispatcherId);
        history.setDispatchType(dispatchType);
        history.setNotes(request.getNotes());
        dispatchHistoryRepository.save(history);

        publishTicketAssignedEvent(updated, technicianId, dispatcherId,
                request.isAutoAssign(), request.getNotes());

        notificationApi.createNotification(
                technicianId,
                updated.id(),
                "ASSIGNMENT",
                "New Ticket Assigned",
                "Ticket " + updated.ticketNumber() + " has been assigned to you");

        if (previousTechnicianId != null && !previousTechnicianId.equals(technicianId)) {
            notificationApi.createNotification(
                    previousTechnicianId,
                    updated.id(),
                    "REASSIGNMENT",
                    "Ticket Reassigned",
                    "Ticket " + updated.ticketNumber() + " has been reassigned to another technician");
        }

        return updated;
    }

    // ── Availability toggle ───────────────────────────────────────────────────

    @Transactional
    public void setTechnicianAvailability(Long userId, boolean available) {
        List<TechnicianSkill> skills = technicianSkillRepository.findByUserId(userId);
        if (skills.isEmpty()) {
            throw new WebException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        skills.forEach(s -> s.setAvailable(available));
        technicianSkillRepository.saveAll(skills);
        log.info("Set technician {} availability to {}", userId, available);
    }

    // ── History queries ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DispatchHistoryResponse> getHistoryByTicket(Long ticketId) {
        return dispatchHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream().map(this::toHistoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DispatchHistoryResponse> getHistoryByTechnician(Long technicianId) {
        return dispatchHistoryRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId)
                .stream().map(this::toHistoryResponse).toList();
    }

    // ── Auto-assign core ──────────────────────────────────────────────────────

    /**
     * Selects the best available technician for the given ticket using the
     * Adaptive Weighted Composite Scoring algorithm.
     *
     * <p><b>Selection pipeline:</b>
     * <ol>
     *   <li>Load all available (available=true) technicians for the category.</li>
     *   <li>Batch-fetch workload, quality scores, and last-assignment times — 3 queries.</li>
     *   <li>Remove candidates at or above their {@code maxConcurrentJobs} limit.
     *       If every candidate is at capacity, fall back to the full pool so the ticket
     *       is never stuck forever (over-capacity assignment is logged as a warning).</li>
     *   <li>Apply a soft skill-preference filter: for CRITICAL tickets prefer EXPERTs;
     *       if none exist, prefer INTERMEDIATEs; for HIGH prefer EXPERT or INTERMEDIATE.
     *       The filter always returns a non-empty list when the input is non-empty, so
     *       no cascading fallback to BEGINNERs is needed.</li>
     *   <li>Score every candidate in the preferred pool with
     *       {@link DispatchScoringService#score} and pick the minimum.</li>
     * </ol>
     */
    private Long autoSelectTechnician(TicketSummary ticket) {
        String severity = ticket.severityLevel() != null ? ticket.severityLevel() : "LOW";

        List<TechnicianSkill> available =
                technicianSkillRepository.findByCategoryIdAndAvailableTrue(ticket.categoryId());
        if (available.isEmpty()) {
            throw new WebException(ErrorCode.NO_QUALIFIED_TECHNICIAN);
        }

        List<Long> ids = available.stream().map(TechnicianSkill::getUserId).toList();

        // ── Batch data gathering (3 queries) ─────────────────────────────────
        Map<Long, Long>          workload      = ticketApi.countActiveTicketsByTechnicianIds(ids);
        Map<Long, Double>        qualityScores = performanceService.computeQualityScores(ids);
        Map<Long, LocalDateTime> lastAssigned  = scoringService.loadLastAssignmentTimes(ids);

        // ── Pool selection ────────────────────────────────────────────────────
        List<TechnicianSkill> underLimit = available.stream()
                .filter(s -> workload.getOrDefault(s.getUserId(), 0L) < s.getMaxConcurrentJobs())
                .toList();

        if (underLimit.isEmpty()) {
            log.warn("All technicians for category {} are at capacity; assigning to least-loaded.",
                    ticket.categoryId());
        }

        List<TechnicianSkill> candidatePool   = underLimit.isEmpty() ? available : underLimit;
        List<TechnicianSkill> preferredPool   = applySeveritySkillFilter(candidatePool, severity);

        // ── Scoring ───────────────────────────────────────────────────────────
        return preferredPool.stream()
                .min(Comparator.comparingDouble(s -> scoringService.score(
                        s,
                        workload.getOrDefault(s.getUserId(), 0L),
                        qualityScores.getOrDefault(s.getUserId(), 0.5),
                        lastAssigned.get(s.getUserId()),
                        ticket
                ).total()))
                .map(TechnicianSkill::getUserId)
                .orElseThrow(() -> new WebException(ErrorCode.NO_QUALIFIED_TECHNICIAN));
    }

    // ── Skill preference filter ───────────────────────────────────────────────

    /**
     * Returns the highest-skill subset of {@code candidates} appropriate for the severity.
     * Always returns a non-empty list when the input is non-empty.
     *
     * <ul>
     *   <li>CRITICAL → EXPERTs; fall back to INTERMEDIATEs; fall back to full input.</li>
     *   <li>HIGH     → EXPERTs or INTERMEDIATEs; fall back to full input.</li>
     *   <li>MEDIUM/LOW → all candidates (skill level is not a routing criterion).</li>
     * </ul>
     *
     * Within the returned pool, the scoring algorithm further differentiates
     * candidates using the {@code skillMismatch} factor.
     */
    private List<TechnicianSkill> applySeveritySkillFilter(
            List<TechnicianSkill> candidates, String severity) {
        return switch (severity) {
            case "CRITICAL" -> {
                var experts = filter(candidates, "EXPERT");
                if (!experts.isEmpty()) yield experts;
                var intermediates = filter(candidates, "INTERMEDIATE");
                yield intermediates.isEmpty() ? candidates : intermediates;
            }
            case "HIGH" -> {
                var upper = candidates.stream()
                        .filter(s -> "EXPERT".equals(s.getSkillLevel())
                                  || "INTERMEDIATE".equals(s.getSkillLevel()))
                        .toList();
                yield upper.isEmpty() ? candidates : upper;
            }
            default -> candidates; // MEDIUM / LOW: accept any skill level
        };
    }

    private List<TechnicianSkill> filter(List<TechnicianSkill> skills, String level) {
        return skills.stream().filter(s -> level.equals(s.getSkillLevel())).toList();
    }

    // ── Manual-assign validation ──────────────────────────────────────────────

    private void validateTechnicianForManualAssign(
            Long technicianId, Long categoryId, boolean forceAssign) {

        TechnicianSkill skill = technicianSkillRepository
                .findByUserIdAndCategoryId(technicianId, categoryId)
                .orElseThrow(() -> new WebException(ErrorCode.TECHNICIAN_NOT_QUALIFIED));

        if (!skill.isAvailable()) {
            throw new WebException(ErrorCode.TECHNICIAN_UNAVAILABLE);
        }

        if (!forceAssign) {
            long activeCount = ticketApi.countActiveTicketsByTechnicianIds(List.of(technicianId))
                    .getOrDefault(technicianId, 0L);
            if (activeCount >= skill.getMaxConcurrentJobs()) {
                throw new WebException(ErrorCode.TECHNICIAN_AT_CAPACITY);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String determineDispatchType(
            boolean isAuto, Long previousTechnicianId, Long newTechnicianId) {
        if (isAuto) return "AUTO";
        if (previousTechnicianId != null
                && !previousTechnicianId.equals(newTechnicianId)) return "REASSIGNMENT";
        return "MANUAL";
    }

    private void publishTicketAssignedEvent(TicketSummary ticket, Long technicianId,
            Long dispatcherId, boolean isAuto, String notes) {
        try {
            TicketAssignedEvent event = TicketAssignedEvent.builder()
                    .ticketId(ticket.id())
                    .ticketNumber(ticket.ticketNumber())
                    .technicianId(technicianId)
                    .dispatcherId(dispatcherId)
                    .dispatchType(isAuto ? "AUTO" : "MANUAL")
                    .notes(notes)
                    .assignedAt(LocalDateTime.now())
                    .build();
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket assigned event", e);
        }
    }

    private DispatchHistoryResponse toHistoryResponse(DispatchHistory h) {
        return DispatchHistoryResponse.builder()
                .id(h.getId())
                .ticketId(h.getTicketId())
                .technicianId(h.getTechnicianId())
                .previousTechnicianId(h.getPreviousTechnicianId())
                .dispatcherId(h.getDispatcherId())
                .dispatchType(h.getDispatchType())
                .notes(h.getNotes())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
