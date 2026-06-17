package com.example.facility.dispatch.consumer;

import com.example.facility.shared.event.TicketAssignedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TicketAssignedEventConsumer {

    // Notification to the technician is already sent synchronously in DispatchService.assignTicket().
    // This listener handles the async audit trail only.
    @ApplicationModuleListener
    public void onTicketAssigned(TicketAssignedEvent event) {
        try {
            log.info("Ticket assigned audit: ticketId={}, ticketNumber={}, technicianId={}, " +
                     "dispatcherId={}, type={}, at={}",
                    event.getTicketId(), event.getTicketNumber(), event.getTechnicianId(),
                    event.getDispatcherId(), event.getDispatchType(), event.getAssignedAt());
        } catch (Exception e) {
            log.error("Error processing ticket assigned event for ticketId={}", event.getTicketId(), e);
        }
    }
}
