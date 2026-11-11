package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.service.LedgerService;
import com.ledgerline.backend.service.SecurityMonitor;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private final LedgerService ledger;
    private final SecurityMonitor monitor;

    public TransactionController(LedgerService ledger, SecurityMonitor monitor) {
        this.ledger = ledger; this.monitor = monitor;
    }

    @PostMapping("/deposit")
    public TransactionResponse deposit(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") String key,
                                       @Valid @RequestBody DepositRequest req) {
        Long actor = actorId(jwt);
        return run(actor, key, "DEPOSIT", () -> ledger.deposit(actor, key, req));
    }

    @PostMapping("/withdraw")
    public TransactionResponse withdraw(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") String key,
                                        @Valid @RequestBody WithdrawRequest req) {
        Long actor = actorId(jwt);
        return run(actor, key, "WITHDRAWAL", () -> ledger.withdraw(actor, key, req));
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") String key,
                                        @Valid @RequestBody TransferRequest req) {
        Long actor = actorId(jwt);
        return run(actor, key, "TRANSFER", () -> ledger.transfer(actor, key, req));
    }

    private TransactionResponse run(Long actor, String key, String action, Supplier<TransactionResponse> op) {
        if (!KEY.matcher(key).matches()) {
            throw ApiException.badRequest("Idempotency-Key must be 8-64 characters: letters, digits, - or _");
        }
        try {
            return op.get();
        } catch (DataIntegrityViolationException dup) {
            // Two identical requests raced: the DB unique constraint stopped the second one.
            return ledger.findByKey(actor, key).orElseThrow(() -> dup);
        } catch (ApiException e) {
            monitor.transactionFailed(actor, action, e, key);   // audit, suspicious-pattern check and failure notice
            throw e;
        }
    }

    private Long actorId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}