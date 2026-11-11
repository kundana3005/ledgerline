package com.ledgerline.backend.service;

import com.ledgerline.backend.entity.AuditLog;
import com.ledgerline.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository repo;
        public String clientIp() { return currentIp(); }
    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    /** Joins the caller's transaction: if the money movement rolls back, so does this record. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(Long actorId, String action, String entityType, String entityId, Map<String, ?> details) {
        save(actorId, action, entityType, entityId, details);
    }

    /** Own transaction: survives even when the caller's transaction rolled back (failures, logins). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIndependent(Long actorId, String action, String entityType, String entityId, Map<String, ?> details) {
        save(actorId, action, entityType, entityId, details);
    }

    private void save(Long actorId, String action, String entityType, String entityId, Map<String, ?> details) {
        repo.save(new AuditLog(actorId, action, entityType, entityId, toJson(details), currentIp()));
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }

    private String toJson(Map<String, ?> m) {
        if (m == null || m.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(esc(e.getKey())).append("\":\"").append(esc(String.valueOf(e.getValue()))).append('"');
        }
        return sb.append('}').toString();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}