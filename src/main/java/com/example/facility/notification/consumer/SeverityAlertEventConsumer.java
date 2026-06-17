package com.example.facility.notification.consumer;

import com.example.facility.identity.IdentityApi;
import com.example.facility.notification.NotificationApi;
import com.example.facility.shared.event.SeverityAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeverityAlertEventConsumer {

    private final NotificationApi notificationApi;
    private final IdentityApi identityApi;

    // FR-NOTIF-06: notify all ADMIN users when a ticket's severity level escalates
    @ApplicationModuleListener
    public void onSeverityAlert(SeverityAlertEvent event) {
        try {
            log.warn("Severity escalation for ticket {}: {} → {}",
                    event.getTicketNumber(), event.getPreviousSeverityLevel(), event.getNewSeverityLevel());

            identityApi.findUserIdsByRole("ADMIN").forEach(adminId ->
                    notificationApi.createNotification(
                            adminId,
                            event.getTicketId(),
                            "SEVERITY_ALERT",
                            "Severity Escalation Alert",
                            "Ticket " + event.getTicketNumber() + " severity escalated from "
                                    + event.getPreviousSeverityLevel() + " to "
                                    + event.getNewSeverityLevel()
                                    + " (score: " + String.format("%.1f", event.getNewSeverityScore()) + ")"
                    ));
        } catch (Exception e) {
            log.error("Error processing severity alert for ticket {}", event.getTicketNumber(), e);
        }
    }
}
