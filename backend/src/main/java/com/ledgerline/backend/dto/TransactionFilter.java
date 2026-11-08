package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.TransactionStatus;
import com.ledgerline.backend.entity.TransactionType;
import java.time.LocalDate;

public record TransactionFilter(String search, TransactionType type, TransactionStatus status,
                                LocalDate from, LocalDate to) {}