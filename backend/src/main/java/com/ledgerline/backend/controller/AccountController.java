package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.AccountResponse;
import com.ledgerline.backend.repository.AccountRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRepository accounts;

    public AccountController(AccountRepository accounts) { this.accounts = accounts; }

    @GetMapping
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return accounts.findByOwnerIdOrderByIdAsc(userId).stream().map(AccountResponse::from).toList();
    }
}