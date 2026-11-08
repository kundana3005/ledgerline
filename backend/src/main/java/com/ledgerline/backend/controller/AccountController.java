package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.AccountResponse;
import com.ledgerline.backend.dto.StatementResponse;
import com.ledgerline.backend.repository.AccountRepository;
import com.ledgerline.backend.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRepository accounts;
    private final ReportService reports;

    public AccountController(AccountRepository accounts, ReportService reports) {
        this.accounts = accounts; this.reports = reports;
    }

    @GetMapping
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return accounts.findByOwnerIdOrderByIdAsc(userId).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}/statement")
    public StatementResponse statement(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.statement(Long.valueOf(jwt.getSubject()), id, from, to);
    }
}