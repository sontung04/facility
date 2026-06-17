package com.example.facility.ticket.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.ticket.TicketApi;
import com.example.facility.ticket.TicketSummary;
import com.example.facility.ticket.model.SeverityLevel;
import com.example.facility.ticket.model.Ticket;
import com.example.facility.ticket.model.TicketStatus;
import com.example.facility.ticket.repository.ResolutionFeedbackRepository;
import com.example.facility.ticket.repository.TicketRepository;
import com.example.facility.ticket.repository.TicketSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketApiImpl implements TicketApi {

    private final TicketRepository ticketRepository;
    private final ResolutionFeedbackRepository feedbackRepository;

    private static final List<TicketStatus> CLOSED_STATUSES =
            List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    // ── Dispatch ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TicketSummary getTicketById(Long ticketId) {
        return toSummary(ticketRepository.findById(ticketId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @Override
    @Transactional
    public TicketSummary performAssignment(Long ticketId, Long technicianId, String notes) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        ticket.setAssignedTechnicianId(technicianId);
        ticket.setAssignmentNotes(notes);
        ticket.setAssignedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.ASSIGNED);
        return toSummary(ticketRepository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countActiveTicketsByTechnicianIds(List<Long> technicianIds) {
        return ticketRepository
                .countActiveTicketsByTechnicianIds(technicianIds, CLOSED_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));
    }

    // ── TechnicianPerformanceService ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TicketSummary> findByTechnicianIdAndStatusIn(Long technicianId, List<String> statuses) {
        List<TicketStatus> statusList = statuses.stream().map(TicketStatus::valueOf).toList();
        return ticketRepository.findByAssignedTechnicianIdAndStatusIn(technicianId, statusList)
                .stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countPerformanceMetricsByTechnicianIds(List<Long> technicianIds) {
        return ticketRepository.countPerformanceMetricsByTechnicianIds(technicianIds, CLOSED_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> avgRatingByTechnicianIds(List<Long> technicianIds) {
        return feedbackRepository.avgRatingByTechnicianIds(technicianIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Double avgRatingByTechnicianId(Long technicianId) {
        return feedbackRepository.avgRatingByTechnicianId(technicianId);
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TicketSummary> findForAnalytics(Long categoryId, Long buildingId,
            String severity, LocalDateTime startDate, LocalDateTime endDate) {
        SeverityLevel severityLevel = null;
        if (severity != null && !severity.isBlank()) {
            try { severityLevel = SeverityLevel.valueOf(severity.toUpperCase()); }
            catch (IllegalArgumentException ignored) { /* unknown value → no filter */ }
        }
        return ticketRepository
                .findAll(TicketSpecification.withAnalyticsFilters(
                        categoryId, buildingId, severityLevel, startDate, endDate))
                .stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countVolumeByDay(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.countVolumeByDay(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countVolumeByWeek(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.countVolumeByWeek(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countVolumeByMonth(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.countVolumeByMonth(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMttrByCategory(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.getMttrByCategory(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMttrByTechnician(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.getMttrByTechnician(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMttrByBuilding(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.getMttrByBuilding(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.findBySubmittedAtBetween(startDate, endDate).size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatusIn(List<String> statuses) {
        List<TicketStatus> statusList = statuses.stream().map(TicketStatus::valueOf).toList();
        return ticketRepository.findByStatusIn(statusList).size();
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    TicketSummary toSummary(Ticket ticket) {
        return new TicketSummary(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getStatus().toString(),
                ticket.getSeverityLevel().toString(),
                ticket.getSeverityScore(),
                ticket.getDescription(),
                ticket.getCategory().getId(),
                ticket.getCategory().getName(),
                ticket.getDevice().getId(),
                ticket.getDevice().getDeviceCode(),
                ticket.getReportedBy(),
                ticket.getAssignedTechnicianId(),
                ticket.getAssignmentNotes(),
                ticket.getSubmittedAt(),
                ticket.getAckAt(),
                ticket.getAssignedAt(),
                ticket.getInProgressAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getSlaAckBreached(),
                ticket.getSlaResolveBreached(),
                ticket.getSlaClosureBreached()
        );
    }
}
