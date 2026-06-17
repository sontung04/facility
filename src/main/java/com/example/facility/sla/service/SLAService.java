package com.example.facility.sla.service;

import com.example.facility.notification.NotificationApi;
import com.example.facility.shared.event.SLABreachEvent;
import com.example.facility.sla.model.SLABreach;
import com.example.facility.sla.model.SLAPolicy;
import com.example.facility.sla.repository.SLABreachRepository;
import com.example.facility.sla.repository.SLAPolicyRepository;
import com.example.facility.ticket.TicketCreatedEvent;
import com.example.facility.ticket.model.Ticket;
import com.example.facility.ticket.model.TicketStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SLAService {

    private final SLAPolicyRepository slaPolicyRepository;
    private final SLABreachRepository slaBreachRepository;
    private final NotificationApi notificationApi;
    private final ApplicationEventPublisher eventPublisher;
    /**
     * Used solely to obtain a JPA proxy reference via {@code getReference(Ticket.class, id)}
     * when persisting SLABreach.ticket. No business queries are issued.
     */
    private final EntityManager entityManager;

    // ── TicketCreatedEvent listener ───────────────────────────────────────────

    /**
     * Initializes SLA breach records after a ticket has been committed to the DB.
     * Fires asynchronously after the creating transaction commits.
     */
    @ApplicationModuleListener
    public void onTicketCreated(TicketCreatedEvent event) {
        try {
            Optional<SLAPolicy> policyOptional =
                    slaPolicyRepository.findByCategoryIdAndIsActiveTrue(event.categoryId());

            if (policyOptional.isEmpty()) {
                log.warn("No active SLA policy found for categoryId: {}", event.categoryId());
                return;
            }

            SLAPolicy policy = policyOptional.get();
            LocalDateTime submittedAt = event.submittedAt();

            // JPA proxy reference — does not load the full Ticket entity
            Ticket ticketRef = entityManager.getReference(Ticket.class, event.ticketId());

            slaBreachRepository.save(SLABreach.builder()
                    .ticket(ticketRef)
                    .breachType(SLABreach.BreachType.ACK_BREACH)
                    .expectedBy(submittedAt.plusMinutes(policy.getAcknowledgmentTimeMinutes()))
                    .isBreached(false)
                    .build());

            slaBreachRepository.save(SLABreach.builder()
                    .ticket(ticketRef)
                    .breachType(SLABreach.BreachType.RESOLVE_BREACH)
                    .expectedBy(submittedAt.plusMinutes(policy.getResolutionTimeMinutes()))
                    .isBreached(false)
                    .build());

            slaBreachRepository.save(SLABreach.builder()
                    .ticket(ticketRef)
                    .breachType(SLABreach.BreachType.CLOSURE_BREACH)
                    .expectedBy(submittedAt.plusMinutes(policy.getClosureTimeMinutes()))
                    .isBreached(false)
                    .build());

            log.info("SLA policies initialized for ticket {}", event.ticketId());
        } catch (Exception e) {
            log.error("Error initializing SLA for ticket {}", event.ticketId(), e);
        }
    }

    // ── Scheduled breach detection ────────────────────────────────────────────

    /**
     * Scheduled job to detect SLA breaches every 10 minutes.
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void detectAndProcessSLABreaches() {
        try {
            log.info("Running SLA breach detection job...");
            LocalDateTime now = LocalDateTime.now();
            List<SLABreach> pendingBreaches = slaBreachRepository.findPendingBreaches(now);

            for (SLABreach breach : pendingBreaches) {
                Ticket ticket = breach.getTicket();
                boolean isBreached = switch (breach.getBreachType()) {
                    case ACK_BREACH     -> ticket.getStatus() != TicketStatus.ACK;
                    case RESOLVE_BREACH -> ticket.getStatus() != TicketStatus.RESOLVED
                                       && ticket.getStatus() != TicketStatus.CLOSED;
                    case CLOSURE_BREACH -> ticket.getStatus() != TicketStatus.CLOSED;
                };
                if (isBreached) {
                    processSLABreach(breach);
                }
            }

            log.info("SLA breach detection completed. Checked {} pending entries", pendingBreaches.size());
        } catch (Exception e) {
            log.error("Error in SLA breach detection job", e);
        }
    }

    @Transactional
    void processSLABreach(SLABreach breach) {
        breach.setIsBreached(true);
        breach.setActualBreachAt(LocalDateTime.now());
        slaBreachRepository.save(breach);

        Ticket ticket = breach.getTicket();

        // Reflect the breach back onto the Ticket's denormalized boolean flags so that
        // analytics queries (slaResolveBreached), performance metrics, and the Ticket
        // response all see the correct breach state.  The Ticket entity is managed in
        // the current transaction (lazy-loaded via @ManyToOne), so dirty-checking will
        // flush this update automatically — no separate save call needed.
        switch (breach.getBreachType()) {
            case ACK_BREACH     -> ticket.setSlaAckBreached(true);
            case RESOLVE_BREACH -> ticket.setSlaResolveBreached(true);
            case CLOSURE_BREACH -> ticket.setSlaClosureBreached(true);
        }

        SLABreachEvent event = SLABreachEvent.builder()
                .ticketId(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .breachType(breach.getBreachType().toString())
                .severityLevel(ticket.getSeverityLevel().toString())
                .breachedAt(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(event);

        if (ticket.getReportedBy() != null) {
            notificationApi.createNotification(
                    ticket.getReportedBy(),
                    ticket.getId(),
                    "SLA_BREACH",
                    "SLA Breach Alert",
                    "SLA breach detected: " + breach.getBreachType()
                            + " for ticket " + ticket.getTicketNumber());
        }

        log.warn("SLA breach processed: ticket={}, type={}", ticket.getId(), breach.getBreachType());
    }

    public Long getTotalSLABreaches() {
        return slaBreachRepository.countBreachedSLAs();
    }
}
