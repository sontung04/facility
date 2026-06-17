package com.example.facility.ticket.service;

import com.example.facility.facility.CategoryInfo;
import com.example.facility.facility.DeviceInfo;
import com.example.facility.facility.FacilityApi;
import com.example.facility.facility.model.Category;
import com.example.facility.facility.model.Device;
import com.example.facility.notification.NotificationApi;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.shared.event.SeverityAlertEvent;
import com.example.facility.shared.event.TicketStatusChangedEvent;
import com.example.facility.shared.util.SecurityUtils;
import com.example.facility.shared.util.TicketNumberGenerator;
import com.example.facility.ticket.TicketCreatedEvent;
import com.example.facility.ticket.dto.request.CreateTicketRequest;
import com.example.facility.ticket.dto.request.UpdateTicketStatusRequest;
import com.example.facility.ticket.dto.response.CommentResponse;
import com.example.facility.ticket.dto.response.TicketResponse;
import com.example.facility.ticket.model.*;
import com.example.facility.ticket.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final FacilityApi facilityApi;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final NotificationApi notificationApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentRepository commentRepository;
    private final SecurityUtils securityUtils;
    private final EntityManager entityManager;

    @Value("${app.facility.deduplication.window-hours:24}")
    private Integer deduplicationWindowHours;

    // L2: valid forward transitions — ASSIGNED is terminal here (only DispatchService sets it)
    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.SUBMITTED,   Set.of(TicketStatus.ACK),
            TicketStatus.ACK,         Set.of(),
            TicketStatus.ASSIGNED,    Set.of(TicketStatus.IN_PROGRESS),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED),
            TicketStatus.RESOLVED,    Set.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED,      Set.of()
    );

    // L1: which statuses each role may set via this endpoint
    private static final Map<String, Set<TicketStatus>> ROLE_ALLOWED_STATUSES = Map.of(
            "TECHNICIAN", Set.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            "ADMIN",      Set.of(TicketStatus.ACK, TicketStatus.CLOSED)
    );

    /**
     * Manager creates a ticket directly for a device in their room.
     * Enforces room-manager ownership. If an open ticket for the same device +
     * category already exists (within the dedup window) the request is rejected
     * with INVALID_REQUEST so the manager uses the existing ticket instead.
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Long managerId) {
        // Validate via FacilityApi (no facility repository injection needed)
        DeviceInfo deviceInfo = facilityApi.getDevice(request.getDeviceId());

        if (!facilityApi.isManagerOfRoom(deviceInfo.roomId(), managerId)) {
            throw new WebException(ErrorCode.NOT_ROOM_MANAGER);
        }

        CategoryInfo categoryInfo = facilityApi.getCategory(request.getCategoryId());

        // Prevent duplicate open tickets for the same device+category within the window
        LocalDateTime windowStart = LocalDateTime.now().minusHours(deduplicationWindowHours);
        List<Ticket> existing = ticketRepository
                .findByDeviceIdAndCategoryIdAndStatusInAndSubmittedAtAfter(
                        request.getDeviceId(),
                        request.getCategoryId(),
                        List.of(TicketStatus.SUBMITTED, TicketStatus.ACK,
                                TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS),
                        windowStart);
        if (!existing.isEmpty()) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }

        // Obtain JPA proxy references — avoids loading entities from facility module repositories
        Device deviceRef   = entityManager.getReference(Device.class, deviceInfo.id());
        Category categoryRef = entityManager.getReference(Category.class, categoryInfo.id());

        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumberGenerator.generateTicketNumber())
                .category(categoryRef)
                .device(deviceRef)
                .status(TicketStatus.SUBMITTED)
                .description(request.getDescription())
                .severityScore(request.getSeverityScore())
                .severityLevel(SeverityCalculator.calculateSeverityLevel(request.getSeverityScore()))
                .reportedBy(managerId)
                .build();
        ticket = ticketRepository.save(ticket);
        log.info("Created ticket {} for device {}", ticket.getTicketNumber(), deviceInfo.deviceCode());

        // Notify all ADMINs immediately when a CRITICAL ticket is created
        if (ticket.getSeverityLevel() == SeverityLevel.CRITICAL) {
            eventPublisher.publishEvent(SeverityAlertEvent.builder()
                    .ticketId(ticket.getId())
                    .ticketNumber(ticket.getTicketNumber())
                    .previousSeverityLevel(null)
                    .newSeverityLevel(ticket.getSeverityLevel().name())
                    .previousSeverityScore(null)
                    .newSeverityScore(ticket.getSeverityScore())
                    .escalatedAt(LocalDateTime.now())
                    .build());
        }

        // Publish event so SLAService can initialize breach records asynchronously
        eventPublisher.publishEvent(new TicketCreatedEvent(
                ticket.getId(), ticket.getTicketNumber(),
                categoryInfo.id(), managerId, ticket.getSubmittedAt()));

        return mapToResponse(ticket, deviceInfo, categoryInfo);
    }

    @Transactional
    public TicketResponse updateTicketStatus(UpdateTicketStatusRequest request, Long currentUserId) {
        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        TicketStatus newStatus;
        try {
            newStatus = TicketStatus.valueOf(request.getNewStatus());
        } catch (IllegalArgumentException e) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }

        String role = securityUtils.getCurrentUserRole();

        // L1: role must be permitted to set this status
        if (!ROLE_ALLOWED_STATUSES.getOrDefault(role, Set.of()).contains(newStatus)) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }

        // L2: transition must be valid from the current status (also blocks ASSIGNED/SUBMITTED — L4)
        if (!ALLOWED_TRANSITIONS.getOrDefault(ticket.getStatus(), Set.of()).contains(newStatus)) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }

        // L3: technician may only update tickets assigned to them
        if ("TECHNICIAN".equals(role) && !currentUserId.equals(ticket.getAssignedTechnicianId())) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }

        String oldStatus = ticket.getStatus().toString();
        ticket.setStatus(newStatus);
        switch (newStatus) {
            case ACK         -> ticket.setAckAt(LocalDateTime.now());
            case IN_PROGRESS -> ticket.setInProgressAt(LocalDateTime.now());
            case RESOLVED    -> ticket.setResolvedAt(LocalDateTime.now());
            case CLOSED      -> ticket.setClosedAt(LocalDateTime.now());
            default          -> { }
        }
        ticket = ticketRepository.save(ticket);

        // L5: persist status-change notes as an auto-generated comment
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            Comment comment = new Comment();
            comment.setTicketId(ticket.getId());
            comment.setUserId(currentUserId);
            comment.setContent("[Status → " + newStatus + "] " + request.getNotes());
            commentRepository.save(comment);
        }

        publishTicketStatusChangedEvent(ticket, oldStatus, newStatus, currentUserId);

        notificationApi.createNotification(
                ticket.getAssignedTechnicianId() != null ? ticket.getAssignedTechnicianId() : currentUserId,
                ticket.getId(),
                "STATUS_CHANGE",
                "Ticket Status Updated",
                "Ticket " + ticket.getTicketNumber() + " status changed to " + newStatus);

        if (ticket.getReportedBy() != null && !ticket.getReportedBy().equals(currentUserId)) {
            notificationApi.createNotification(
                    ticket.getReportedBy(),
                    ticket.getId(),
                    "STATUS_CHANGE",
                    "Your Ticket Status Updated",
                    "Ticket " + ticket.getTicketNumber() + " status changed to " + newStatus);
        }

        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long ticketId) {
        return mapToResponse(ticketRepository.findById(ticketId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getTicketsByStatus(String status, Pageable pageable) {
        try {
            return ticketRepository.findByStatus(TicketStatus.valueOf(status), pageable)
                    .map(this::mapToResponse);
        } catch (IllegalArgumentException e) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getTechnicianTickets(Long technicianId, Pageable pageable) {
        return ticketRepository.findByAssignedTechnicianId(technicianId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getManagerTickets(Long managerId, Pageable pageable) {
        return ticketRepository.findByReportedBy(managerId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> searchTickets(String status, Long deviceId, Long categoryId,
            String severity, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        TicketStatus ticketStatus = null;
        if (status != null) {
            try { ticketStatus = TicketStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new WebException(ErrorCode.INVALID_REQUEST); }
        }
        SeverityLevel severityLevel = null;
        if (severity != null) {
            try { severityLevel = SeverityLevel.valueOf(severity.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new WebException(ErrorCode.INVALID_REQUEST); }
        }
        return ticketRepository.findAll(
                        TicketSpecification.withFilters(ticketStatus, deviceId, categoryId,
                                severityLevel, startDate, endDate),
                        pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, String text) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        Long commenterId = securityUtils.getCurrentUserId();

        Comment comment = new Comment();
        comment.setTicketId(ticketId);
        comment.setUserId(commenterId);
        comment.setContent(text);
        Comment saved = commentRepository.save(comment);

        Set<Long> recipients = new LinkedHashSet<>();
        if (ticket.getAssignedTechnicianId() != null) recipients.add(ticket.getAssignedTechnicianId());
        if (ticket.getReportedBy() != null)           recipients.add(ticket.getReportedBy());
        recipients.remove(commenterId);

        recipients.forEach(userId -> notificationApi.createNotification(
                userId, ticketId, "COMMENT",
                "New Comment on Ticket",
                "A new comment was added to ticket " + ticket.getTicketNumber()));

        return CommentResponse.builder()
                .id(saved.getId())
                .ticketId(saved.getTicketId())
                .userId(saved.getUserId())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    public List<CommentResponse> getTicketComments(Long ticketId) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        return commentRepository.findByTicketId(ticketId).stream()
                .map(c -> CommentResponse.builder()
                        .id(c.getId())
                        .ticketId(c.getTicketId())
                        .userId(c.getUserId())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void publishTicketStatusChangedEvent(Ticket ticket, String oldStatus,
            TicketStatus newStatus, Long userId) {
        try {
            TicketStatusChangedEvent event = TicketStatusChangedEvent.builder()
                    .ticketId(ticket.getId())
                    .ticketNumber(ticket.getTicketNumber())
                    .oldStatus(oldStatus)
                    .newStatus(newStatus.toString())
                    .changedByUserId(userId)
                    .changedAt(LocalDateTime.now())
                    .build();
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket status changed event", e);
        }
    }

    /**
     * Full mapper when we already have DeviceInfo/CategoryInfo in scope (avoids re-loading lazily).
     */
    private TicketResponse mapToResponse(Ticket ticket, DeviceInfo dev, CategoryInfo cat) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus().toString())
                .severityLevel(ticket.getSeverityLevel().toString())
                .severityScore(ticket.getSeverityScore())
                .description(ticket.getDescription())
                .deviceId(dev.id())
                .deviceCode(dev.deviceCode())
                .categoryId(cat.id())
                .categoryName(cat.name())
                .reportedBy(ticket.getReportedBy())
                .assignedTechnicianId(ticket.getAssignedTechnicianId())
                .assignmentNotes(ticket.getAssignmentNotes())
                .submittedAt(ticket.getSubmittedAt())
                .ackAt(ticket.getAckAt())
                .assignedAt(ticket.getAssignedAt())
                .inProgressAt(ticket.getInProgressAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .slaAckBreached(ticket.getSlaAckBreached())
                .slaResolveBreached(ticket.getSlaResolveBreached())
                .slaClosureBreached(ticket.getSlaClosureBreached())
                .build();
    }

    /** Standard mapper — accesses device/category via lazy JPA. Valid within @Transactional. */
    TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus().toString())
                .severityLevel(ticket.getSeverityLevel().toString())
                .severityScore(ticket.getSeverityScore())
                .description(ticket.getDescription())
                .deviceId(ticket.getDevice().getId())
                .deviceCode(ticket.getDevice().getDeviceCode())
                .categoryId(ticket.getCategory().getId())
                .categoryName(ticket.getCategory().getName())
                .reportedBy(ticket.getReportedBy())
                .assignedTechnicianId(ticket.getAssignedTechnicianId())
                .assignmentNotes(ticket.getAssignmentNotes())
                .submittedAt(ticket.getSubmittedAt())
                .ackAt(ticket.getAckAt())
                .assignedAt(ticket.getAssignedAt())
                .inProgressAt(ticket.getInProgressAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .slaAckBreached(ticket.getSlaAckBreached())
                .slaResolveBreached(ticket.getSlaResolveBreached())
                .slaClosureBreached(ticket.getSlaClosureBreached())
                .build();
    }
}
