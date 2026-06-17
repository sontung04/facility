package com.example.facility.notification.consumer;

import com.example.facility.identity.IdentityApi;
import com.example.facility.notification.NotificationApi;
import com.example.facility.shared.event.SLABreachEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SLABreachAdminNotificationConsumer {

    private final NotificationApi notificationApi;
    private final IdentityApi identityApi;

    @ApplicationModuleListener
    public void onSLABreach(SLABreachEvent event) {
        try {
            identityApi.findUserIdsByRole("ADMIN").forEach(adminId ->
                    notificationApi.createNotification(
                            adminId,
                            event.getTicketId(),
                            "SLA_BREACH",
                            "SLA Breach Alert",
                            "SLA breach detected: " + event.getBreachType()
                                    + " for ticket " + event.getTicketNumber()
                                    + " (severity: " + event.getSeverityLevel() + ")"
                    ));
        } catch (Exception e) {
            log.error("Error processing SLA breach admin notification for ticketId={}", event.getTicketId(), e);
        }
    }
}
