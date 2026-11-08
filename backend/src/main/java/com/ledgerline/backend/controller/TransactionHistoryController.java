package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.TransactionStatus;
import com.ledgerline.backend.entity.TransactionType;
import com.ledgerline.backend.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionHistoryController {
    private final ReportService reports;

    public TransactionHistoryController(ReportService reports) { this.reports = reports; }

    @GetMapping
    public PageResponse<TransactionSummary> history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // The owner id always comes from the verified token, never from the request
        return reports.searchTransactions(Long.valueOf(jwt.getSubject()),
                new TransactionFilter(search, type, status, from, to), page, size);
    }
}