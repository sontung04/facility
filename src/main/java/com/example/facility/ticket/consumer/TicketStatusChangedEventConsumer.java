package com.example.facility.ticket.consumer;

import com.example.facility.shared.event.TicketStatusChangedEvent;
import com.example.facility.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketStatusChangedEventConsumer {

    private final TicketRepository ticketRepository;

    @ApplicationModuleListener
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        try {
            log.info("Ticket status changed: ticketId={}, {} -> {} by userId={} at {}",
                    event.getTicketId(), event.getOldStatus(), event.getNewStatus(),
                    event.getChangedByUserId(), event.getChangedAt());

            if (!ticketRepository.existsById(event.getTicketId())) {
                log.warn("Received status-change event for unknown ticketId={}", event.getTicketId());
            }
        } catch (Exception e) {
            log.error("Error processing ticket status changed event for ticketId={}",
                    event.getTicketId(), e);
        }
    }
}
