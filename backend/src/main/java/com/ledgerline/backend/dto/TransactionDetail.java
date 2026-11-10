package com.ledgerline.backend.dto;

import java.util.List;

public record TransactionDetail(TransactionSummary summary, List<LedgerEntryView> entries) {}