package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.AccountResponse;
import com.ledgerline.backend.dto.StatementResponse;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.AccountRepository;
import com.ledgerline.backend.service.AuditService;
import com.ledgerline.backend.service.ReportService;
import com.ledgerline.backend.service.StatementExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRepository accounts;
    private final ReportService reports;
    private final StatementExportService export;
    private final AuditService audit;

    public AccountController(AccountRepository accounts, ReportService reports,
                             StatementExportService export, AuditService audit) {
        this.accounts = accounts; this.reports = reports; this.export = export; this.audit = audit;
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

    @GetMapping("/{id}/statement/export")
    public ResponseEntity<byte[]> exportStatement(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        if (!pdf && !"csv".equalsIgnoreCase(format)) throw ApiException.badRequest("format must be csv or pdf");
        Long userId = Long.valueOf(jwt.getSubject());
        StatementResponse s = reports.statement(userId, id, from, to);   // ownership is checked here (404 otherwise)
        byte[] body = pdf ? export.pdf(s) : export.csv(s);
        audit.logIndependent(userId, "STATEMENT_DOWNLOADED", "ACCOUNT", s.accountNumber(),
                Map.of("format", pdf ? "pdf" : "csv"));
        String name = "statement-" + s.accountNumber() + "-" + s.from() + "_" + s.to() + (pdf ? ".pdf" : ".csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}