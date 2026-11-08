package com.ledgerline.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatementResponse(String accountNumber, String accountType, LocalDate from, LocalDate to,
                                BigDecimal openingBalance, BigDecimal closingBalance,
                                BigDecimal totalCredits, BigDecimal totalDebits,
                                List<StatementLine> entries) {}