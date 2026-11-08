package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.AuditLog;
import org.springframework.data.repository.Repository;

public interface AuditLogRepository extends Repository<AuditLog, Long> {
    AuditLog save(AuditLog log);
}