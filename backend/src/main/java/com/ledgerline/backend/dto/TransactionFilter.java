package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.TransactionStatus;
import com.ledgerline.backend.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionFilter(String search, Long id, TransactionType type, TransactionStatus status,
                                LocalDate from, LocalDate to, BigDecimal minAmount, BigDecimal maxAmount,
                                String sort, String dir) {}