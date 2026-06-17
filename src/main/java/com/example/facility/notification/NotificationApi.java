package com.example.facility.notification;

/**
 * Public API of the Notification module.
 * Other modules must go through this interface — never inject NotificationService directly.
 */
public interface NotificationApi {

    /**
     * Creates a persistent notification for the given user.
     *
     * @param userId   recipient user ID
     * @param ticketId related ticket ID (may be null)
     * @param type     notification category (e.g. ASSIGNMENT, STATUS_CHANGE, COMMENT, SLA_BREACH)
     * @param title    short title shown in the UI
     * @param message  full notification body
     */
    void createNotification(Long userId, Long ticketId, String type, String title, String message);
}
