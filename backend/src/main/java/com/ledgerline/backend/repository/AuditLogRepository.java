package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog log);

    @Query(value = "select l from AuditLog l where lower(l.action) like :q " +
                   "or lower(coalesce(l.entityId, '')) like :q or lower(coalesce(l.details, '')) like :q",
           countQuery = "select count(l) from AuditLog l where lower(l.action) like :q " +
                   "or lower(coalesce(l.entityId, '')) like :q or lower(coalesce(l.details, '')) like :q")
    Page<AuditLog> search(@Param("q") String q, Pageable pageable);
}