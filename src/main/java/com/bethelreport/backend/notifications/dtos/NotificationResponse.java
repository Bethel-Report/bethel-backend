package com.bethelreport.backend.notifications.dtos;

import com.bethelreport.backend.notifications.entities.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, String title, String content,
    NotificationType sourceType, Long resourceId, Long bethelId, LocalDateTime createdAt,
    boolean seen, LocalDateTime readAt) {
}
