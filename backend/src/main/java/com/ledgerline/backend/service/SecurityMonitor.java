package com.ledgerline.backend.service;

import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Records failures and flags suspicious patterns. Never receives passwords or tokens. */
@Service
public class SecurityMonitor {
    private static final int LOGIN_THRESHOLD = 5;
    private static final int TX_THRESHOLD = 5;
    private static final List<String> TX_FAILURES = List.of("DEPOSIT_FAILED", "WITHDRAWAL_FAILED", "TRANSFER_FAILED");

    private final AuditService audit;
    private final AuditLogRepository auditRepo;
    private final NotificationService notifications;

    public SecurityMonitor(AuditService audit, AuditLogRepository auditRepo, NotificationService notifications) {
        this.audit = audit; this.auditRepo = auditRepo; this.notifications = notifications;
    }

    public void failedLogin(Long actorId, String maskedIdentifier) {
        audit.logIndependent(actorId, "LOGIN_FAILED", "USER", actorId == null ? null : String.valueOf(actorId),
                Map.of("identifier", maskedIdentifier));
        Instant since = Instant.now().minus(Duration.ofMinutes(15));
        String ip = audit.clientIp();
        long byIp = ip == null ? 0 : auditRepo.countByActionAndIpAddressAndCreatedAtAfter("LOGIN_FAILED", ip, since);
        long byUser = actorId == null ? 0 : auditRepo.countByActionAndActorIdAndCreatedAtAfter("LOGIN_FAILED", actorId, since);
        if (byIp == LOGIN_THRESHOLD || byUser == LOGIN_THRESHOLD) {   // exactly once per burst
            audit.logIndependent(actorId, "LOGIN_BURST_DETECTED", "USER", actorId == null ? null : String.valueOf(actorId),
                    Map.of("failures", String.valueOf(LOGIN_THRESHOLD), "windowMinutes", "15"));
        }
    }

    public void transactionFailed(Long actor, String action, ApiException e, String key) {
        audit.logIndependent(actor, action + "_FAILED", "TRANSACTION", null, Map.of("reason", e.getCode(), "key", key));
        if ("NOT_FOUND".equals(e.getCode()) || "IDEMPOTENCY_CONFLICT".equals(e.getCode())) {
            audit.logIndependent(actor, "SUSPICIOUS_ACTIVITY", "TRANSACTION", null,
                    Map.of("reason", e.getCode(), "action", action));
        }
        long recent = auditRepo.countByActorIdAndActionInAndCreatedAtAfter(actor, TX_FAILURES,
                Instant.now().minus(Duration.ofMinutes(10)));
        if (recent == TX_THRESHOLD) {
            audit.logIndependent(actor, "REPEATED_FAILED_TRANSACTIONS", "USER", String.valueOf(actor),
                    Map.of("failures", String.valueOf(TX_THRESHOLD), "windowMinutes", "10"));
        }
        notifications.notifyIndependent(actor, "TRANSACTION_FAILED",
                "Your " + action.toLowerCase() + " could not be completed: " + e.getMessage() + ".");
    }
}