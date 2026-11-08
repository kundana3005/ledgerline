package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryView(Long id, String transactionReference, String accountNumber, String direction,
                              BigDecimal amount, BigDecimal balanceAfter, Instant createdAt) {
    public static LedgerEntryView from(LedgerEntry e) {
        return new LedgerEntryView(e.getId(), e.getTransaction().getReference(),
                e.getAccount().getAccountNumber(), e.getDirection().name(),
                e.getAmount(), e.getBalanceAfter(), e.getCreatedAt());
    }
}