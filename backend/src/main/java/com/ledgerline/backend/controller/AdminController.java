package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.service.AdminService;
import com.ledgerline.backend.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final ReportService reports;

    public AdminController(AdminService admin, ReportService reports) {
        this.admin = admin; this.reports = reports;
    }

    @GetMapping("/users")
    public List<UserSummary> users() { return admin.listUsers(); }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary createUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest req) {
        return admin.createUser(Long.valueOf(jwt.getSubject()), req);
    }

    @PutMapping("/users/{id}/enabled")
    public UserSummary setEnabled(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody SetEnabledRequest req) {
        return admin.setUserEnabled(Long.valueOf(jwt.getSubject()), id, req.enabled());
    }

    @GetMapping("/accounts")
    public List<AdminAccountView> accounts() { return admin.listAccounts(); }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAccountView createAccount(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CreateAccountRequest req) {
        return admin.createAccount(Long.valueOf(jwt.getSubject()), req);
    }

    @PutMapping("/accounts/{id}/status")
    public AdminAccountView setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @Valid @RequestBody SetAccountStatusRequest req) {
        return admin.setAccountStatus(Long.valueOf(jwt.getSubject()), id, req.status());
    }

    @GetMapping("/stats")
    public AdminStats stats() { return reports.stats(); }
}