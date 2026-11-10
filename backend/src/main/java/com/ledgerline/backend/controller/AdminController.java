package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.AccountRequestStatus;
import com.ledgerline.backend.entity.Role;
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

    private static Long id(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @GetMapping("/users")
    public PageResponse<UserSummary> users(@RequestParam(required = false) String search,
                                           @RequestParam(required = false) Role role,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return admin.searchUsers(search, role, status, page, size);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary createUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest req) {
        return admin.createUser(id(jwt), req);
    }

    @PutMapping("/users/{id}/enabled")
    public UserSummary setEnabled(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody SetEnabledRequest req) {
        return admin.setUserEnabled(id(jwt), id, req.enabled());
    }

    @PutMapping("/users/{id}/approval")
    public UserSummary reviewRegistration(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody ReviewRequest req) {
        return admin.reviewRegistration(id(jwt), id, req.approved(), req.reason());
    }

    @GetMapping("/account-requests")
    public PageResponse<AccountRequestAdminView> accountRequests(
            @RequestParam(defaultValue = "PENDING") AccountRequestStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return admin.listAccountRequests(status, page, size);
    }

    @PutMapping("/account-requests/{id}/decision")
    public AccountRequestAdminView reviewAccountRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                        @Valid @RequestBody ReviewRequest req) {
        return admin.reviewAccountRequest(id(jwt), id, req.approved(), req.reason());
    }

    @GetMapping("/accounts")
    public List<AdminAccountView> accounts() { return admin.listAccounts(); }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAccountView createAccount(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CreateAccountRequest req) {
        return admin.createAccount(id(jwt), req);
    }

    @PutMapping("/accounts/{id}/status")
    public AdminAccountView setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @Valid @RequestBody SetAccountStatusRequest req) {
        return admin.setAccountStatus(id(jwt), id, req.status());
    }

    @GetMapping("/stats")
    public AdminStats stats() { return reports.stats(); }
}