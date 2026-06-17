package com.example.facility.notification.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.notification.model.Notification;
import com.example.facility.notification.repository.NotificationRepository;
import com.example.facility.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.facility.notification.NotificationApi;
import com.example.facility.notification.dto.response.NotificationResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationApi {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    @Value("${app.facility.notification.retention-days:90}")
    private Integer retentionDays;

    @Transactional
    public void createNotification(Long userId, Long ticketId, String type, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTicketId(ticketId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notificationRepository.save(notification);
        log.info("Created notification for user: {} type: {}", userId, type);
    }

    // L5: paginated — replaces the unbounded List overload in the controller
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(boolean unreadOnly, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (unreadOnly) {
            return notificationRepository
                    .findByUserIdAndIsRead(currentUserId, false, pageable)
                    .map(this::mapToResponse);
        }
        return notificationRepository
                .findByUserId(currentUserId, pageable)
                .map(this::mapToResponse);
    }

    // L3: ownership check added
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!notification.getUserId().equals(securityUtils.getCurrentUserId())) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }
        return mapToResponse(notification);
    }

    // L4: ownership check added — used by controller
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!notification.getUserId().equals(securityUtils.getCurrentUserId())) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }
        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(currentUserId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    // L3: ownership check added
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!notification.getUserId().equals(securityUtils.getCurrentUserId())) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }
        notificationRepository.delete(notification);
    }

    // L6: single bulk DELETE statement — no heap allocation
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        notificationRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up notifications older than {}", cutoff);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .ticketId(notification.getTicketId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

