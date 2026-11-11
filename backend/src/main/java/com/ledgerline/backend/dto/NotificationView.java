package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.Notification;
import java.time.Instant;

public record NotificationView(Long id, String type, String message, boolean read, Instant createdAt) {
    public static NotificationView from(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getMessage(), n.isSeen(), n.getCreatedAt());
    }
}