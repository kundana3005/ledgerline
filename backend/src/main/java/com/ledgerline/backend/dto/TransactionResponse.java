package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.Transaction;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(String reference, String type, String status, BigDecimal amount,
                                  String sourceAccount, String destinationAccount,
                                  Instant createdAt, boolean replayed) {
    public static TransactionResponse from(Transaction t, boolean replayed) {
        return new TransactionResponse(t.getReference(), t.getType().name(), t.getStatus().name(),
                t.getAmount(),
                t.getSourceAccount() == null ? null : t.getSourceAccount().getAccountNumber(),
                t.getDestinationAccount() == null ? null : t.getDestinationAccount().getAccountNumber(),
                t.getCreatedAt(), replayed);
    }
}