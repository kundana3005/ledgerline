package com.ledgerline.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementLine(Instant date, String reference, String type, String direction,
                            BigDecimal amount, BigDecimal balanceAfter) {}