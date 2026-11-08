package com.ledgerline.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionSummary(Long id, String reference, String type, String status, BigDecimal amount,
                                 String sourceAccount, String destinationAccount,
                                 String initiatedBy, Instant createdAt) {}