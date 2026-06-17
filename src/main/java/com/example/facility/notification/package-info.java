/**
 * Notification module — creates and delivers in-app notifications.
 *
 * <p>Public API exposed to other modules:
 * <ul>
 *   <li>{@link com.example.facility.notification.NotificationApi}</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification",
        allowedDependencies = { "shared", "identity" }
)
package com.example.facility.notification;
