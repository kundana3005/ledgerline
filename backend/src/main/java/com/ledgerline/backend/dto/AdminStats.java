package com.ledgerline.backend.dto;

import java.math.BigDecimal;

public record AdminStats(long totalUsers, long admins, long accountHolders, long auditors,
                         long pendingRegistrations, long pendingAccountRequests,
                         long customerAccounts, long activeAccounts, BigDecimal totalCustomerBalance,
                         long totalTransactions, long deposits, long withdrawals, long transfers,
                         BigDecimal totalVolume, BigDecimal totalDebits, BigDecimal totalCredits,
                         boolean ledgerBalanced) {}