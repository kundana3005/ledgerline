package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.AuditLog;
import java.time.Instant;

public record AuditLogView(Long id, Long actorId, String action, String entityType, String entityId,
                           String details, String ipAddress, Instant createdAt) {
    public static AuditLogView from(AuditLog l) {
        return new AuditLogView(l.getId(), l.getActorId(), l.getAction(), l.getEntityType(),
                l.getEntityId(), l.getDetails(), l.getIpAddress(), l.getCreatedAt());
    }
}