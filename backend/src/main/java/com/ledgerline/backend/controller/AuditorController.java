package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.TransactionStatus;
import com.ledgerline.backend.entity.TransactionType;
import com.ledgerline.backend.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/auditor")
public class AuditorController {
    private final ReportService reports;

    public AuditorController(ReportService reports) { this.reports = reports; }

        @GetMapping("/transactions")
    public PageResponse<TransactionSummary> transactions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reports.searchTransactions(null,
                new TransactionFilter(search, id, type, status, from, to, minAmount, maxAmount, sort, dir), page, size);
    }

        @GetMapping("/ledger")
    public PageResponse<LedgerEntryView> ledger(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reports.ledgerEntries(search, account, type, from, to, page, size);
    }

    @GetMapping("/transactions/{reference}")
    public TransactionDetail transaction(@PathVariable String reference) {
        return reports.transactionDetail(reference);
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogView> auditLogs(@RequestParam(required = false) String search,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return reports.auditLogs(search, page, size);
    }

    @GetMapping("/summary")
    public AdminStats summary() { return reports.stats(); }
}