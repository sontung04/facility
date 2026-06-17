package com.example.facility.sla.consumer;

import com.example.facility.shared.event.SLABreachEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SLABreachEventConsumer {

    @ApplicationModuleListener
    public void onSLABreach(SLABreachEvent event) {
        try {
            log.warn("ALERT: SLA Breach — Ticket: {}, Type: {}, Severity: {}, Breached at: {}",
                    event.getTicketNumber(), event.getBreachType(),
                    event.getSeverityLevel(), event.getBreachedAt());
        } catch (Exception e) {
            log.error("Error processing SLA breach event for ticketId={}", event.getTicketId(), e);
        }
    }
}
